package pe.edu.utp.Grupo06.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import pe.edu.utp.Grupo06.model.DetalleVenta;

import java.util.List;

@Repository
public interface DetalleVentaRepository extends JpaRepository<DetalleVenta, Long> {
    List<DetalleVenta> findByVentaId(Long ventaId);

    // Consulta para productos de mayor rotación (RF08)
    @Query("SELECT d.producto.id, d.producto.nombre, SUM(d.cantidad) AS totalVendido " +
            "FROM DetalleVenta d " +
            "GROUP BY d.producto.id, d.producto.nombre " +
            "ORDER BY totalVendido DESC")
    List<Object[]> findProductosMayorRotacion();
}