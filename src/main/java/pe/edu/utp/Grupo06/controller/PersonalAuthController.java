package pe.edu.utp.Grupo06.controller;

import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import pe.edu.utp.Grupo06.dto.auth.LoginRequestDTO;
import pe.edu.utp.Grupo06.dto.auth.LoginResponseDTO;
import pe.edu.utp.Grupo06.dto.usuario.UsuarioResponseDTO;
import pe.edu.utp.Grupo06.model.Empleado;
import pe.edu.utp.Grupo06.service.IEmpleadoService;
import pe.edu.utp.Grupo06.service.TokenService;

@RestController
@RequestMapping("/api/personal/auth")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class PersonalAuthController {

    private final IEmpleadoService empleadoService;
    private final TokenService tokens;

    public PersonalAuthController(IEmpleadoService empleadoService, TokenService tokens) {
        this.empleadoService = empleadoService;
        this.tokens = tokens;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO datos) {
        Empleado e = empleadoService.validarCredenciales(datos.getUsername(), datos.getPassword());
        return ResponseEntity.ok(respuesta(e));
    }

    @GetMapping("/me")
    public UsuarioResponseDTO perfil(Authentication authentication) {
        Empleado e = empleadoService.buscarPorId(Long.parseLong(authentication.getName()));
        return new UsuarioResponseDTO(e.getId(), e.getUsername(), e.getNombreCompleto(), e.getEmail(),
                e.getTelefono(), e.getActivo(), e.getRol());
    }

    private LoginResponseDTO respuesta(Empleado e) {
        return new LoginResponseDTO(e.getId(), e.getUsername(), e.getNombreCompleto(), e.getEmail(),
                e.getRol(), "Autenticación exitosa", tokens.emitirParaEmpleado(e), "Bearer", TokenService.DURACION_SEGUNDOS);
    }
}
