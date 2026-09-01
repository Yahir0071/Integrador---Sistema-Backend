package pe.edu.utp.Grupo06.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.utp.Grupo06.model.DetalleVenta;
import pe.edu.utp.Grupo06.model.Pago;
import pe.edu.utp.Grupo06.model.Producto;
import pe.edu.utp.Grupo06.model.Venta;
import pe.edu.utp.Grupo06.model.enums.EstadoVenta;
import pe.edu.utp.Grupo06.model.enums.TipoMovimiento;
import pe.edu.utp.Grupo06.repository.DetalleVentaRepository;
import pe.edu.utp.Grupo06.repository.ProductoRepository;
import pe.edu.utp.Grupo06.repository.VentaRepository;
import pe.edu.utp.Grupo06.service.IMovimientoInventarioService;
import pe.edu.utp.Grupo06.service.IVentaService;
import pe.edu.utp.Grupo06.util.Validador;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class VentaServiceImpl implements IVentaService {

    @Autowired
    private VentaRepository ventaRepository;

    @Autowired
    private DetalleVentaRepository detalleVentaRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private IMovimientoInventarioService movimientoService;

    @Autowired
    private Validador validador;

    @Override
    @Transactional
    public Venta registrarVenta(Venta venta) {
        if (venta.getDetalles() == null || venta.getDetalles().isEmpty()) {
            throw new RuntimeException("La venta debe contener al menos un detalle");
        }
        if (venta.getPagos() == null || venta.getPagos().isEmpty()) {
            throw new RuntimeException("La venta debe registrar al menos un método de pago");
        }

        BigDecimal totalCalculado = BigDecimal.ZERO;
        venta.setFechaVenta(LocalDateTime.now());
        venta.setEstado(EstadoVenta.EMITIDA);

        for (DetalleVenta detalle : venta.getDetalles()) {
            validador.validar(detalle);

            Producto producto = productoRepository.findById(detalle.getProducto().getId())
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado ID: " + detalle.getProducto().getId()));

            if (producto.getStockActual() < detalle.getCantidad()) {
                throw new RuntimeException("Stock insuficiente para el producto: " + producto.getNombre() +
                        ". Disponible: " + producto.getStockActual() + ", Requerido: " + detalle.getCantidad());
            }

            detalle.setPrecioUnitario(producto.getPrecioVenta());
            BigDecimal subtotal = producto.getPrecioVenta().multiply(BigDecimal.valueOf(detalle.getCantidad()));
            detalle.setSubtotal(subtotal);
            detalle.setVenta(venta);

            totalCalculado = totalCalculado.add(subtotal);
        }

        venta.setTotal(totalCalculado);

        // RNF06 (integridad): la suma de los pagos (efectivo + Yape + Plin, etc.)
        // debe coincidir exactamente con el total calculado de la venta.
        BigDecimal totalPagado = BigDecimal.ZERO;
        for (Pago pago : venta.getPagos()) {
            if (pago.getMonto() == null || pago.getMonto().compareTo(BigDecimal.ZERO) <= 0) {
                throw new RuntimeException("Cada pago debe tener un monto mayor a 0");
            }
            pago.setVenta(venta);
            totalPagado = totalPagado.add(pago.getMonto());
        }

        if (totalPagado.compareTo(totalCalculado) != 0) {
            throw new RuntimeException("El total de los pagos (" + totalPagado +
                    ") no coincide con el total de la venta (" + totalCalculado + ")");
        }

        // Recién aquí, con la venta validada por completo, se registran los
        // movimientos de salida de stock (si algo falla antes de este punto,
        // no se descuenta stock de nada).
        for (DetalleVenta detalle : venta.getDetalles()) {
            movimientoService.registrarMovimiento(
                    detalle.getProducto().getId(),
                    venta.getUsuario().getId(),
                    TipoMovimiento.SALIDA,
                    detalle.getCantidad(),
                    "Venta con ticket: " + venta.getNumeroTicket()
            );
        }

        return ventaRepository.save(venta);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Venta> listarVentas() {
        return ventaRepository.findAllConUsuario();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Venta> listarPorFechas(LocalDateTime inicio, LocalDateTime fin) {
        return ventaRepository.findByFechaVentaBetweenOrderByFechaVentaDesc(inicio, fin);
    }

    @Override
    @Transactional(readOnly = true)
    public Venta buscarPorId(Long id) {
        return ventaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Venta no encontrada con ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public Venta buscarPorTicket(String ticket) {
        return ventaRepository.findByNumeroTicket(ticket)
                .orElseThrow(() -> new RuntimeException("Venta no encontrada con ticket: " + ticket));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Object[]> reporteMayorRotacion() {
        return detalleVentaRepository.findProductosMayorRotacion();
    }

    @Override
    @Transactional
    public Venta anularVenta(Long ventaId, Long usuarioId, String motivo) {
        Venta venta = buscarPorId(ventaId);

        if (venta.getEstado() == EstadoVenta.ANULADA) {
            throw new RuntimeException("La venta con ticket " + venta.getNumeroTicket() + " ya se encuentra anulada");
        }

        // Devuelve al stock la cantidad de cada producto vendido.
        for (DetalleVenta detalle : venta.getDetalles()) {
            movimientoService.registrarMovimiento(
                    detalle.getProducto().getId(),
                    usuarioId,
                    TipoMovimiento.ENTRADA,
                    detalle.getCantidad(),
                    "Anulación de venta con ticket: " + venta.getNumeroTicket() +
                            (motivo != null && !motivo.isBlank() ? " — Motivo: " + motivo : "")
            );
        }

        venta.setEstado(EstadoVenta.ANULADA);
        return ventaRepository.save(venta);
    }
}
