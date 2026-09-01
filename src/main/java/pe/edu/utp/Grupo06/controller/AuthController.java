package pe.edu.utp.Grupo06.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.utp.Grupo06.dto.auth.LoginRequestDTO;
import pe.edu.utp.Grupo06.dto.auth.LoginResponseDTO;
import pe.edu.utp.Grupo06.model.Usuario;
import pe.edu.utp.Grupo06.service.IUsuarioService;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private IUsuarioService usuarioService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
        Usuario usuario = usuarioService.validarCredenciales(request.getUsername(), request.getPassword());
        
        LoginResponseDTO response = new LoginResponseDTO(
                usuario.getId(),
                usuario.getUsername(),
                usuario.getNombreCompleto(),
                usuario.getEmail(),
                usuario.getRol().getNombre(),
                "Autenticación exitosa"
        );

        return ResponseEntity.ok(response);
    }
}
