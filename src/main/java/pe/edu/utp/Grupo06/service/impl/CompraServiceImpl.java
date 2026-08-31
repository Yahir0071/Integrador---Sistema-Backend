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

    @Override
    @Transactional
    public Compra registrarCompra(Compra compra) {
        if (compra.getDetalles() == null || compra.getDetalles().isEmpty()) {
            throw new RuntimeException("La compra debe incluir al menos un detalle");
        }

        BigDecimal totalCalculado = BigDecimal.ZERO;
        compra.setFechaCompra(LocalDateTime.now());

        for (DetalleCompra detalle : compra.getDetalles()) {
            Producto producto = productoRepository.findById(detalle.getProducto().getId())
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado ID: " + detalle.getProducto().getId()));

            BigDecimal subtotal = detalle.getPrecioUnitario().multiply(BigDecimal.valueOf(detalle.getCantidad()));
            detalle.setSubtotal(subtotal);
            detalle.setCompra(compra);

            totalCalculado = totalCalculado.add(subtotal);

            // Registrar movimiento de entrada de stock y refrescar alerta de reposición
            movimientoService.registrarMovimiento(
                    producto.getId(),
                    compra.getUsuario().getId(),
                    TipoMovimiento.ENTRADA,
                    detalle.getCantidad(),
                    "Compra de proveedor con comprobante: " + compra.getNumeroComprobante()
            );
        }

        compra.setTotal(totalCalculado);
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