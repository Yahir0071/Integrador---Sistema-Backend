package pe.edu.utp.Grupo06.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import pe.edu.utp.Grupo06.model.Venta;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface VentaRepository extends JpaRepository<Venta, Long> {
    Optional<Venta> findByNumeroTicket(String numeroTicket);
    @EntityGraph(attributePaths = {"usuario"})
    List<Venta> findByFechaVentaBetweenOrderByFechaVentaDesc(LocalDateTime inicio, LocalDateTime fin);

    @EntityGraph(attributePaths = {"usuario"})
    List<Venta> findByUsuarioIdOrderByFechaVentaDesc(Long usuarioId);

    @EntityGraph(attributePaths = {"usuario"})
    @Query("SELECT v FROM Venta v")
    List<Venta> findAllConUsuario();
}