package pe.edu.utp.Grupo06.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.utp.Grupo06.model.Empleado;
import pe.edu.utp.Grupo06.model.enums.RolNombre;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmpleadoRepository extends JpaRepository<Empleado, Long> {

    Optional<Empleado> findByUsername(String username);

    Optional<Empleado> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    List<Empleado> findByRol(RolNombre rol);

    List<Empleado> findByActivoTrue();
}
