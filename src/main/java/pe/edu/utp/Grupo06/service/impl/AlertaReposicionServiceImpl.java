package pe.edu.utp.Grupo06.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.utp.Grupo06.model.AlertaReposicion;
import pe.edu.utp.Grupo06.model.Producto;
import pe.edu.utp.Grupo06.model.enums.EstadoAlerta;
import pe.edu.utp.Grupo06.repository.AlertaReposicionRepository;
import pe.edu.utp.Grupo06.repository.ProductoRepository;
import pe.edu.utp.Grupo06.service.IAlertaReposicionService;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AlertaReposicionServiceImpl implements IAlertaReposicionService {

    @Autowired
    private AlertaReposicionRepository alertaRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Override
    @Transactional
    public void verificarYGenerarAlerta(Long productoId) {
        Producto producto = productoRepository.findById(productoId).orElse(null);
        if (producto == null || !producto.getEstado()) return;

        // Si el stock actual es menor o igual al stock mínimo (RF05)
        if (producto.getStockActual() <= producto.getStockMinimo()) {
            boolean yaExistePendiente = alertaRepository.existsByProductoIdAndEstado(productoId, EstadoAlerta.PENDIENTE);
            if (!yaExistePendiente) {
                AlertaReposicion alerta = new AlertaReposicion();
                alerta.setProducto(producto);
                alerta.setStockRegistrado(producto.getStockActual());
                alerta.setStockMinimo(producto.getStockMinimo());
                // Sugerencia base: reponer al menos el doble del stock mínimo
                alerta.setCantidadSugerida(Math.max((producto.getStockMinimo() * 2) - producto.getStockActual(), producto.getStockMinimo()));
                alerta.setEstado(EstadoAlerta.PENDIENTE);
                alerta.setFechaGeneracion(LocalDateTime.now());
                alerta.setObservacion("Generada automáticamente por quiebre/bajo stock");
                alertaRepository.save(alerta);
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<AlertaReposicion> listarPendientes() {
        return alertaRepository.findByEstadoOrderByFechaGeneracionDesc(EstadoAlerta.PENDIENTE);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AlertaReposicion> listarPorEstado(EstadoAlerta estado) {
        return alertaRepository.findByEstadoOrderByFechaGeneracionDesc(estado);
    }

    @Override
    @Transactional
    public AlertaReposicion atenderAlerta(Long alertaId, String observacion) {
        AlertaReposicion alerta = alertaRepository.findById(alertaId)
                .orElseThrow(() -> new RuntimeException("Alerta no encontrada con ID: " + alertaId));
        alerta.setEstado(EstadoAlerta.ATENDIDA);
        alerta.setObservacion(observacion != null ? observacion : "Alerta atendida correctamente");
        return alertaRepository.save(alerta);
    }

    @Override
    @Transactional
    public AlertaReposicion descartarAlerta(Long alertaId) {
        AlertaReposicion alerta = alertaRepository.findById(alertaId)
                .orElseThrow(() -> new RuntimeException("Alerta no encontrada con ID: " + alertaId));
        alerta.setEstado(EstadoAlerta.DESCARTADA);
        return alertaRepository.save(alerta);
    }
}