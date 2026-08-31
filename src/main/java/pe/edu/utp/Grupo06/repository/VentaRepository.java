package pe.edu.utp.Grupo06.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.utp.Grupo06.model.Venta;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface VentaRepository extends JpaRepository<Venta, Long> {
    Optional<Venta> findByNumeroTicket(String numeroTicket);
    List<Venta> findByFechaVentaBetweenOrderByFechaVentaDesc(LocalDateTime inicio, LocalDateTime fin);
    List<Venta> findByUsuarioIdOrderByFechaVentaDesc(Long usuarioId);
}