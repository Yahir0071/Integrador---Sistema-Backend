package pe.edu.utp.Grupo06.repository;
import pe.edu.utp.Grupo06.model.ComprobantePedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Collection;
import java.util.Set;
public interface ComprobantePedidoRepository extends JpaRepository<ComprobantePedido,Long> {
 boolean existsByHash(String hash);
 @Query("select c.pedidoId from ComprobantePedido c where c.pedidoId in :ids")
 Set<Long> findPedidoIdsByPedidoIdIn(@Param("ids") Collection<Long> ids);
}
