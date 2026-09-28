package pe.edu.utp.Grupo06.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import pe.edu.utp.Grupo06.model.Rol;
import pe.edu.utp.Grupo06.model.enums.RolNombre;
import pe.edu.utp.Grupo06.repository.RolRepository;

@Configuration
public class DataInitializerConfig {

    @Bean
    CommandLineRunner inicializarRoles(RolRepository rolRepository) {
        return args -> {
            for (RolNombre rolNombre : RolNombre.values()) {
                if (rolRepository.findByNombre(rolNombre).isEmpty()) {
                    Rol rol = new Rol();
                    rol.setNombre(rolNombre);
                    rolRepository.save(rol);
                    System.out.println(" Rol inicializado en BD: " + rolNombre);
                }
            }
        };
    }

    @Bean
    CommandLineRunner actualizarRestriccionesBd(JdbcTemplate jdbcTemplate) {
        return args -> {
            try {
                jdbcTemplate.execute("ALTER TABLE movimientos_inventario DROP CONSTRAINT IF EXISTS movimientos_inventario_tipo_movimiento_check");
                jdbcTemplate.execute("ALTER TABLE movimientos_inventario ADD CONSTRAINT movimientos_inventario_tipo_movimiento_check " +
                        "CHECK (tipo_movimiento IN ('ENTRADA', 'SALIDA', 'AJUSTE', 'REPOSICION', 'MERMA'))");
            } catch (Exception e) {
                System.out.println("Aviso al actualizar restricciones de movimientos_inventario: " + e.getMessage());
            }
        };
    }
}
