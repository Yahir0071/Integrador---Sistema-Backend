package pe.edu.utp.Grupo06.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.utp.Grupo06.model.Usuario;
import pe.edu.utp.Grupo06.repository.UsuarioRepository;
import pe.edu.utp.Grupo06.service.IUsuarioService;
import pe.edu.utp.Grupo06.util.Validador;

import java.util.List;

@Service
public class UsuarioServiceImpl implements IUsuarioService {

    private static final int PASSWORD_MIN_LENGTH = 6;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private Validador validador;

    @Override
    @Transactional(readOnly = true)
    public List<Usuario> listarTodos() {
        return usuarioRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Usuario> listarActivos() {
        return usuarioRepository.findByActivoTrue();
    }

    @Override
    @Transactional(readOnly = true)
    public Usuario buscarPorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public Usuario buscarPorUsername(String username) {
        return usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con username: " + username));
    }

    @Override
    @Transactional
    public Usuario registrar(Usuario usuario) {
        if (usuarioRepository.existsByUsername(usuario.getUsername())) {
            throw new RuntimeException("El nombre de usuario ya se encuentra registrado: " + usuario.getUsername());
        }

        validarPasswordEnTextoPlano(usuario.getPassword());

        // Se encripta ANTES de validar la entidad completa (la validación de
        // @NotBlank sobre el campo password sigue pasando porque el hash
        // nunca queda vacío) y ANTES de guardar.
        usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));

        validador.validar(usuario);

        return usuarioRepository.save(usuario);
    }

    @Override
    @Transactional
    public Usuario actualizar(Long id, Usuario usuarioActualizado) {
        Usuario existente = buscarPorId(id);
        existente.setNombreCompleto(usuarioActualizado.getNombreCompleto());
        existente.setEmail(usuarioActualizado.getEmail());
        existente.setTelefono(usuarioActualizado.getTelefono());
        existente.setRol(usuarioActualizado.getRol());

        if (usuarioActualizado.getPassword() != null && !usuarioActualizado.getPassword().isBlank()) {
            validarPasswordEnTextoPlano(usuarioActualizado.getPassword());
            existente.setPassword(passwordEncoder.encode(usuarioActualizado.getPassword()));
        }

        validador.validar(existente);

        return usuarioRepository.save(existente);
    }

    @Override
    @Transactional
    public void cambiarEstado(Long id, Boolean activo) {
        Usuario usuario = buscarPorId(id);
        usuario.setActivo(activo);
        usuarioRepository.save(usuario);
    }

    @Override
    @Transactional(readOnly = true)
    public Usuario validarCredenciales(String username, String password) {
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario o contraseña incorrectos"));

        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            throw new RuntimeException("El usuario se encuentra inactivo");
        }

        if (!passwordEncoder.matches(password, usuario.getPassword())) {
            throw new RuntimeException("Usuario o contraseña incorrectos");
        }

        return usuario;
    }

    private void validarPasswordEnTextoPlano(String password) {
        if (password == null || password.isBlank()) {
            throw new RuntimeException("La contraseña es obligatoria");
        }
        if (password.length() < PASSWORD_MIN_LENGTH) {
            throw new RuntimeException("La contraseña debe tener al menos " + PASSWORD_MIN_LENGTH + " caracteres");
        }
    }
}
