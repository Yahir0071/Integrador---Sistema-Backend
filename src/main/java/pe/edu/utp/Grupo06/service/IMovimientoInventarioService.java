package pe.edu.utp.Grupo06.service;

import pe.edu.utp.Grupo06.model.MovimientoInventario;
import pe.edu.utp.Grupo06.model.enums.TipoMovimiento;
import java.time.LocalDateTime;
import java.util.List;

public interface IMovimientoInventarioService {
    MovimientoInventario registrarMovimiento(Long productoId, Long usuarioId, TipoMovimiento tipo, Integer cantidad, String motivo);
    List<MovimientoInventario> listarPorProducto(Long productoId);
    List<MovimientoInventario> listarPorRangoFechas(LocalDateTime inicio, LocalDateTime fin);
    List<MovimientoInventario> listarPorTipo(TipoMovimiento tipo);
}