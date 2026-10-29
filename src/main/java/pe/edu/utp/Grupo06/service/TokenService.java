package pe.edu.utp.Grupo06.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import pe.edu.utp.Grupo06.config.ApiSecurityConfig;
import pe.edu.utp.Grupo06.model.Cliente;
import pe.edu.utp.Grupo06.model.Empleado;
import pe.edu.utp.Grupo06.model.Usuario;
import java.time.Instant;

@Service
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class TokenService {
    public static final long DURACION_SEGUNDOS = 3600;
    private final JwtEncoder encoder;
    public TokenService(JwtEncoder encoder) { this.encoder = encoder; }

    public String emitir(Usuario usuario) {
        Instant ahora = Instant.now();
        String tipo = (usuario.getRol() != null && usuario.getRol().getNombre() != null 
                && usuario.getRol().getNombre().name().equals("CLIENTE")) ? "CLIENTE" : "EMPLEADO";
        String rol = (usuario.getRol() != null && usuario.getRol().getNombre() != null) 
                ? usuario.getRol().getNombre().name() : "CLIENTE";
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer(ApiSecurityConfig.ISSUER)
                .subject(usuario.getId().toString())
                .claim("tipo", tipo)
                .claim("rol", rol)
                .issuedAt(ahora)
                .expiresAt(ahora.plusSeconds(DURACION_SEGUNDOS)).build();
        return encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }

    public String emitirParaCliente(Cliente cliente) {
        Instant ahora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer(ApiSecurityConfig.ISSUER)
                .subject(cliente.getId().toString())
                .claim("tipo", "CLIENTE")
                .claim("rol", "CLIENTE")
                .issuedAt(ahora)
                .expiresAt(ahora.plusSeconds(DURACION_SEGUNDOS)).build();
        return encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }

    public String emitirParaEmpleado(Empleado empleado) {
        Instant ahora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer(ApiSecurityConfig.ISSUER)
                .subject(empleado.getId().toString())
                .claim("tipo", "EMPLEADO")
                .claim("rol", empleado.getRol().name())
                .issuedAt(ahora)
                .expiresAt(ahora.plusSeconds(DURACION_SEGUNDOS)).build();
        return encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }
}
