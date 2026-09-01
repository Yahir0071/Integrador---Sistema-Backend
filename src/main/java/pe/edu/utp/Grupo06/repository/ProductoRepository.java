package pe.edu.utp.Grupo06.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import pe.edu.utp.Grupo06.model.Producto;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {

    @EntityGraph(attributePaths = {"categoria", "proveedor"})
    List<Producto> findByEstadoTrue();

    @EntityGraph(attributePaths = {"categoria", "proveedor"})
    List<Producto> findByCategoriaIdAndEstadoTrue(Long categoriaId);

    Optional<Producto> findByCodigo(String codigo);

    @EntityGraph(attributePaths = {"categoria", "proveedor"})
    List<Producto> findByNombreContainingIgnoreCaseAndEstadoTrue(String nombre);

    @EntityGraph(attributePaths = {"categoria", "proveedor"})
    @Query("SELECT p FROM Producto p WHERE p.stockActual <= p.stockMinimo AND p.estado = true")
    List<Producto> findProductosConBajoStock();
}
