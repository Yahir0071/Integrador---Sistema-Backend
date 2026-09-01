package pe.edu.utp.Grupo06.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.utp.Grupo06.model.Compra;
import pe.edu.utp.Grupo06.model.DetalleCompra;
import pe.edu.utp.Grupo06.model.Producto;
import pe.edu.utp.Grupo06.model.enums.TipoMovimiento;
import pe.edu.utp.Grupo06.repository.CompraRepository;
import pe.edu.utp.Grupo06.repository.ProductoRepository;
import pe.edu.utp.Grupo06.service.ICompraService;
import pe.edu.utp.Grupo06.service.IMovimientoInventarioService;
import pe.edu.utp.Grupo06.util.Validador;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class CompraServiceImpl implements ICompraService {

    @Autowired
    private CompraRepository compraRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private IMovimientoInventarioService movimientoService;

    @Autowired
    private Validador validador;

    @Override
    @Transactional
    public Compra registrarCompra(Compra compra) {
        if (compra.getDetalles() == null || compra.getDetalles().isEmpty()) {
            throw new RuntimeException("La compra debe incluir al menos un detalle");
        }

        BigDecimal totalCalculado = BigDecimal.ZERO;
        compra.setFechaCompra(LocalDateTime.now());

        // Primero se valida y calcula todo; recién al final se registran los
        // movimientos de stock, para no dejar movimientos "sueltos" si algún
        // detalle posterior falla la validación.
        for (DetalleCompra detalle : compra.getDetalles()) {
            validador.validar(detalle);

            Producto producto = productoRepository.findById(detalle.getProducto().getId())
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado ID: " + detalle.getProducto().getId()));

            BigDecimal subtotal = detalle.getPrecioUnitario().multiply(BigDecimal.valueOf(detalle.getCantidad()));
            detalle.setSubtotal(subtotal);
            detalle.setCompra(compra);

            totalCalculado = totalCalculado.add(subtotal);
        }

        compra.setTotal(totalCalculado);

        for (DetalleCompra detalle : compra.getDetalles()) {
            movimientoService.registrarMovimiento(
                    detalle.getProducto().getId(),
                    compra.getUsuario().getId(),
                    TipoMovimiento.ENTRADA,
                    detalle.getCantidad(),
                    "Compra de proveedor con comprobante: " + compra.getNumeroComprobante()
            );
        }

        return compraRepository.save(compra);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Compra> listarCompras() {
        return compraRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Compra> listarPorFechas(LocalDateTime inicio, LocalDateTime fin) {
        return compraRepository.findByFechaCompraBetweenOrderByFechaCompraDesc(inicio, fin);
    }

    @Override
    @Transactional(readOnly = true)
    public Compra buscarPorId(Long id) {
        return compraRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Compra no encontrada con ID: " + id));
    }
}
