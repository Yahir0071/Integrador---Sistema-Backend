package pe.edu.utp.Grupo06.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Expone el PasswordEncoder como bean de Spring para poder inyectarlo
 * en UsuarioServiceImpl (hash de contraseñas) sin necesidad de activar
 * todavía el módulo completo de Spring Security (eso se hará en la fase
 * de autenticación/autorización, junto con los Controllers).
 *
 * Requiere en el pom.xml únicamente:
 *   <dependency>
 *       <groupId>org.springframework.security</groupId>
 *       <artifactId>spring-security-crypto</artifactId>
 *   </dependency>
 */
@Configuration
public class SecurityBeansConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
