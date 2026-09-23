package pe.edu.utp.Grupo06.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import pe.edu.utp.Grupo06.model.Compra;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CompraRepository extends JpaRepository<Compra, Long> {
    @EntityGraph(attributePaths = {"proveedor", "usuario"})
    List<Compra> findByProveedorIdOrderByFechaCompraDesc(Long proveedorId);

    @EntityGraph(attributePaths = {"proveedor", "usuario"})
    List<Compra> findByFechaCompraBetweenOrderByFechaCompraDesc(LocalDateTime inicio, LocalDateTime fin);

    @EntityGraph(attributePaths = {"proveedor", "usuario"})
    @Query("SELECT c FROM Compra c")
    List<Compra> findAllConProveedorYUsuario();

    @EntityGraph(attributePaths = {"proveedor", "usuario"})
    @Query("SELECT c FROM Compra c WHERE c.fechaCompra BETWEEN :inicio AND :fin ORDER BY c.fechaCompra ASC")
    List<Compra> findComprasEntreFechas(@org.springframework.data.repository.query.Param("inicio") LocalDateTime inicio,
                                       @org.springframework.data.repository.query.Param("fin") LocalDateTime fin);

    @Query("SELECT c.proveedor.razonSocial, SUM(c.total) " +
           "FROM Compra c " +
           "WHERE c.fechaCompra BETWEEN :inicio AND :fin " +
           "GROUP BY c.proveedor.razonSocial " +
           "ORDER BY SUM(c.total) DESC")
    List<Object[]> findGastoPorProveedorEntreFechas(@org.springframework.data.repository.query.Param("inicio") LocalDateTime inicio,
                                                  @org.springframework.data.repository.query.Param("fin") LocalDateTime fin);
}