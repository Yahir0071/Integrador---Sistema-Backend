package pe.edu.utp.Grupo06.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.utp.Grupo06.model.DetalleCompra;

import java.util.List;

@Repository
public interface DetalleCompraRepository extends JpaRepository<DetalleCompra, Long> {
    @EntityGraph(attributePaths = {"producto"})
    List<DetalleCompra> findByCompraId(Long compraId);
}
