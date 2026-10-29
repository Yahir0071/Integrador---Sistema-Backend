package pe.edu.utp.Grupo06.controller;

import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import pe.edu.utp.Grupo06.dto.auth.*;
import pe.edu.utp.Grupo06.dto.usuario.UsuarioResponseDTO;
import pe.edu.utp.Grupo06.model.Cliente;
import pe.edu.utp.Grupo06.model.enums.RolNombre;
import pe.edu.utp.Grupo06.repository.EmpleadoRepository;
import pe.edu.utp.Grupo06.service.IClienteService;
import pe.edu.utp.Grupo06.service.TokenService;

@RestController
@RequestMapping("/api/auth")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class AuthController {
    private final IClienteService clienteService;
    private final EmpleadoRepository empleadoRepository;
    private final TokenService tokens;

    public AuthController(IClienteService clienteService, EmpleadoRepository empleadoRepository, TokenService tokens) {
        this.clienteService = clienteService;
        this.empleadoRepository = empleadoRepository;
        this.tokens = tokens;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO datos) {
        if (empleadoRepository.existsByUsername(datos.getUsername())) {
            throw new AccessDeniedException("Esta cuenta pertenece al personal de la bodega. Ingrese desde el portal de gestión.");
        }
        Cliente c = clienteService.validarCredenciales(datos.getUsername(), datos.getPassword());
        return ResponseEntity.ok(respuesta(c));
    }

    @PostMapping("/registro")
    public ResponseEntity<LoginResponseDTO> registrar(@Valid @RequestBody RegistroClienteDTO datos) {
        Cliente c = clienteService.registrar(datos);
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta(c));
    }

    @GetMapping("/me")
    public UsuarioResponseDTO perfil(Authentication authentication) {
        Cliente c = clienteService.buscarPorId(Long.parseLong(authentication.getName()));
        return new UsuarioResponseDTO(c.getId(), c.getUsername(), c.getNombreCompleto(), c.getEmail(),
                c.getTelefono(), c.getActivo(), RolNombre.CLIENTE);
    }

    private LoginResponseDTO respuesta(Cliente c) {
        return new LoginResponseDTO(c.getId(), c.getUsername(), c.getNombreCompleto(), c.getEmail(),
                RolNombre.CLIENTE, "Autenticación exitosa", tokens.emitirParaCliente(c), "Bearer", TokenService.DURACION_SEGUNDOS);
    }
}
