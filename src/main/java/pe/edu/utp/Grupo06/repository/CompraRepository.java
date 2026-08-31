package pe.edu.utp.Grupo06.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.utp.Grupo06.model.Compra;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CompraRepository extends JpaRepository<Compra, Long> {
    List<Compra> findByProveedorIdOrderByFechaCompraDesc(Long proveedorId);
    List<Compra> findByFechaCompraBetweenOrderByFechaCompraDesc(LocalDateTime inicio, LocalDateTime fin);
}