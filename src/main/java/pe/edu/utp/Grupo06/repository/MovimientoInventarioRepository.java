package pe.edu.utp.Grupo06.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.utp.Grupo06.model.MovimientoInventario;
import pe.edu.utp.Grupo06.model.enums.TipoMovimiento;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MovimientoInventarioRepository extends JpaRepository<MovimientoInventario, Long> {
    List<MovimientoInventario> findByProductoIdOrderByFechaMovimientoDesc(Long productoId);
    List<MovimientoInventario> findByTipoMovimientoOrderByFechaMovimientoDesc(TipoMovimiento tipoMovimiento);
    List<MovimientoInventario> findByFechaMovimientoBetweenOrderByFechaMovimientoDesc(LocalDateTime inicio, LocalDateTime fin);
}