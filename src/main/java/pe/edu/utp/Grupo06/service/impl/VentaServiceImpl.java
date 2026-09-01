package pe.edu.utp.Grupo06.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.utp.Grupo06.model.DetalleVenta;
import pe.edu.utp.Grupo06.model.Pago;
import pe.edu.utp.Grupo06.model.Producto;
import pe.edu.utp.Grupo06.model.Venta;
import pe.edu.utp.Grupo06.model.enums.TipoMovimiento;
import pe.edu.utp.Grupo06.repository.DetalleVentaRepository;
import pe.edu.utp.Grupo06.repository.ProductoRepository;
import pe.edu.utp.Grupo06.repository.VentaRepository;
import pe.edu.utp.Grupo06.service.IMovimientoInventarioService;
import pe.edu.utp.Grupo06.service.IVentaService;

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

    @Override
    @Transactional
    public Venta registrarVenta(Venta venta) {
        if (venta.getDetalles() == null || venta.getDetalles().isEmpty()) {
            throw new RuntimeException("La venta debe contener al menos un detalle");
        }

        BigDecimal totalCalculado = BigDecimal.ZERO;
        venta.setFechaVenta(LocalDateTime.now());
        venta.setEstado("EMITIDA");

        for (DetalleVenta detalle : venta.getDetalles()) {
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

            // Registrar movimiento de salida y actualizar stock automáticamente (RF02, RF03)
            movimientoService.registrarMovimiento(
                    producto.getId(),
                    venta.getUsuario().getId(),
                    TipoMovimiento.SALIDA,
                    detalle.getCantidad(),
                    "Venta con ticket: " + venta.getNumeroTicket()
            );
        }

        venta.setTotal(totalCalculado);

        if (venta.getPagos() != null) {
            for (Pago pago : venta.getPagos()) {
                pago.setVenta(venta);
            }
        }

        return ventaRepository.save(venta);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Venta> listarVentas() {
        return ventaRepository.findAll();
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
}