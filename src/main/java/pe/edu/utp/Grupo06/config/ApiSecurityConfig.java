package pe.edu.utp.Grupo06.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.*;
import pe.edu.utp.Grupo06.repository.ClienteRepository;
import pe.edu.utp.Grupo06.repository.EmpleadoRepository;
import pe.edu.utp.Grupo06.repository.UsuarioRepository;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

/** Seguridad HTTP; el contexto de escritorio no crea estos beans. */
@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class ApiSecurityConfig {
    public static final String ISSUER = "sgcivorp-api";

    @Bean
    SecretKey jwtKey(@Value("${app.security.jwt-secret:}") String configured) {
        byte[] bytes;
        if (configured.isBlank()) {
            bytes = new byte[32];
            new SecureRandom().nextBytes(bytes);
            LoggerFactory.getLogger(getClass()).warn("JWT_SECRET no definido: clave aleatoria. Los tokens caducan al reiniciar la API.");
        } else {
            bytes = Base64.getDecoder().decode(configured);
            if (bytes.length < 32) throw new IllegalStateException("JWT_SECRET debe codificar al menos 32 bytes en Base64");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey jwtKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtKey));
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey jwtKey) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(jwtKey).macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(ISSUER));
        return decoder;
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(@Value("${app.security.allowed-origins}") String origins) {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(Arrays.stream(origins.split(",")).map(String::trim).toList());
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        cors.setAllowCredentials(false);
        cors.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cors);
        return source;
    }

    @Bean
    SecurityFilterChain apiSecurity(HttpSecurity http, 
                                   UsuarioRepository usuarios,
                                   ClienteRepository clientes,
                                   EmpleadoRepository empleados,
                                   CorsConfigurationSource corsConfigurationSource) throws Exception {
        http.cors(cors -> cors.configurationSource(corsConfigurationSource))
                // Solo Authorization: Bearer, sin autenticacion por cookies.
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(f -> f.disable()).httpBasic(b -> b.disable()).logout(l -> l.disable())
                .requestCache(c -> c.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/api/catalogo/productos", "/api/catalogo/productos/{id}",
                                "/api/catalogo/categorias", "/api/tienda").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/registro", "/api/personal/auth/login").permitAll()
                        .requestMatchers("/api/auth/me", "/api/personal/auth/me").authenticated()
                        .requestMatchers("/api/personal/**").hasAnyRole("ADMINISTRADOR","VENDEDOR")
                        .requestMatchers("/api/pedidos/**").hasRole("CLIENTE")
                        .requestMatchers("/api/usuarios/**", "/api/empleados/**", "/api/compras/**", "/api/proveedores/**", "/api/reportes/**",
                                "/api/ventas/reporte/**", "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**")
                                .hasRole("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.GET, "/api/productos/**", "/api/categorias/**")
                                .hasAnyRole("ADMINISTRADOR", "VENDEDOR")
                        .requestMatchers("/api/productos/**", "/api/categorias/**").hasRole("ADMINISTRADOR")
                        .requestMatchers("/api/ventas/**", "/api/alertas/**", "/api/mermas/**", "/api/movimientos/**")
                                .hasAnyRole("ADMINISTRADOR", "VENDEDOR")
                        .anyRequest().denyAll())
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, ex) -> {
                            response.setStatus(401);
                            response.setHeader("WWW-Authenticate", "Bearer");
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"status\":401,\"mensaje\":\"Debe iniciar sesión con un token válido\"}");
                        })
                        .accessDeniedHandler((request, response, ex) -> {
                            response.setStatus(403);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"status\":403,\"mensaje\":\"No tiene permisos para esta operación\"}");
                        }))
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(token -> {
                    try {
                        Long id = Long.parseLong(token.getSubject());
                        String tipo = token.getClaimAsString("tipo");

                        if ("CLIENTE".equalsIgnoreCase(tipo)) {
                            var c = clientes.findById(id)
                                    .filter(cl -> Boolean.TRUE.equals(cl.getActivo()))
                                    .orElseThrow(() -> new IllegalArgumentException("Cuenta de cliente no disponible"));
                            return new JwtAuthenticationToken(token, List.of(new SimpleGrantedAuthority("ROLE_CLIENTE")), c.getId().toString());
                        } else if ("EMPLEADO".equalsIgnoreCase(tipo)) {
                            var e = empleados.findById(id)
                                    .filter(emp -> Boolean.TRUE.equals(emp.getActivo()))
                                    .orElseThrow(() -> new IllegalArgumentException("Cuenta de personal no disponible"));
                            return new JwtAuthenticationToken(token, List.of(new SimpleGrantedAuthority("ROLE_" + e.getRol().name())), e.getId().toString());
                        }

                        // Fallback para tokens legados o pruebas unitarias
                        var empFallback = empleados.findById(id).filter(e -> Boolean.TRUE.equals(e.getActivo())).orElse(null);
                        if (empFallback != null) {
                            return new JwtAuthenticationToken(token, List.of(new SimpleGrantedAuthority("ROLE_" + empFallback.getRol().name())), empFallback.getId().toString());
                        }

                        var cliFallback = clientes.findById(id).filter(c -> Boolean.TRUE.equals(c.getActivo())).orElse(null);
                        if (cliFallback != null) {
                            return new JwtAuthenticationToken(token, List.of(new SimpleGrantedAuthority("ROLE_CLIENTE")), cliFallback.getId().toString());
                        }

                        var u = usuarios.findById(id)
                                .filter(user -> Boolean.TRUE.equals(user.getActivo()))
                                .orElseThrow(() -> new IllegalArgumentException("Cuenta no disponible"));
                        return new JwtAuthenticationToken(token, List.of(new SimpleGrantedAuthority(
                                "ROLE_" + u.getRol().getNombre().name())), u.getId().toString());
                    } catch (IllegalArgumentException ex) {
                        throw new OAuth2AuthenticationException(new OAuth2Error("invalid_token"), "Token no válido");
                    }
                })));
        return http.build();
    }
}
