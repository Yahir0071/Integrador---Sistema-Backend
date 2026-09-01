package pe.edu.utp.Grupo06.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.utp.Grupo06.model.AlertaReposicion;
import pe.edu.utp.Grupo06.model.enums.EstadoAlerta;

import java.util.List;

@Repository
public interface AlertaReposicionRepository extends JpaRepository<AlertaReposicion, Long> {
    @EntityGraph(attributePaths = {"producto"})
    List<AlertaReposicion> findByEstadoOrderByFechaGeneracionDesc(EstadoAlerta estado);

    @EntityGraph(attributePaths = {"producto"})
    List<AlertaReposicion> findByProductoIdOrderByFechaGeneracionDesc(Long productoId);
    boolean existsByProductoIdAndEstado(Long productoId, EstadoAlerta estado);


}