package pe.edu.utp.Grupo06.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.utp.Grupo06.model.MovimientoInventario;
import pe.edu.utp.Grupo06.model.Producto;
import pe.edu.utp.Grupo06.model.Usuario;
import pe.edu.utp.Grupo06.model.enums.TipoMovimiento;
import pe.edu.utp.Grupo06.repository.MovimientoInventarioRepository;
import pe.edu.utp.Grupo06.repository.ProductoRepository;
import pe.edu.utp.Grupo06.repository.UsuarioRepository;
import pe.edu.utp.Grupo06.service.IAlertaReposicionService;
import pe.edu.utp.Grupo06.service.IMovimientoInventarioService;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MovimientoInventarioServiceImpl implements IMovimientoInventarioService {

    @Autowired
    private MovimientoInventarioRepository movimientoRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private IAlertaReposicionService alertaReposicionService;

    @Override
    @Transactional
    public MovimientoInventario registrarMovimiento(Long productoId, Long usuarioId, TipoMovimiento tipo, Integer cantidad, String motivo) {
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado con ID: " + productoId));

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + usuarioId));

        int stockAnterior = producto.getStockActual();
        int stockPosterior = stockAnterior;

        if (tipo == TipoMovimiento.ENTRADA || tipo == TipoMovimiento.REPOSICION) {
            stockPosterior += cantidad;
        } else if (tipo == TipoMovimiento.SALIDA) {
            if (stockAnterior < cantidad) {
                throw new RuntimeException("Stock insuficiente para realizar la salida. Stock actual: " + stockAnterior);
            }
            stockPosterior -= cantidad;
        } else if (tipo == TipoMovimiento.AJUSTE) {
            stockPosterior = cantidad;
        }

        producto.setStockActual(stockPosterior);
        productoRepository.save(producto);

        MovimientoInventario movimiento = new MovimientoInventario();
        movimiento.setProducto(producto);
        movimiento.setUsuario(usuario);
        movimiento.setTipoMovimiento(tipo);
        movimiento.setCantidad(cantidad);
        movimiento.setStockAnterior(stockAnterior);
        movimiento.setStockPosterior(stockPosterior);
        movimiento.setFechaMovimiento(LocalDateTime.now());
        movimiento.setMotivo(motivo);

        MovimientoInventario guardado = movimientoRepository.save(movimiento);

        // Disparo automático de validación de reposición (RF03, RF05)
        alertaReposicionService.verificarYGenerarAlerta(producto.getId());

        return guardado;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MovimientoInventario> listarPorProducto(Long productoId) {
        return movimientoRepository.findByProductoIdOrderByFechaMovimientoDesc(productoId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MovimientoInventario> listarPorRangoFechas(LocalDateTime inicio, LocalDateTime fin) {
        return movimientoRepository.findByFechaMovimientoBetweenOrderByFechaMovimientoDesc(inicio, fin);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MovimientoInventario> listarPorTipo(TipoMovimiento tipo) {
        return movimientoRepository.findByTipoMovimientoOrderByFechaMovimientoDesc(tipo);
    }
}