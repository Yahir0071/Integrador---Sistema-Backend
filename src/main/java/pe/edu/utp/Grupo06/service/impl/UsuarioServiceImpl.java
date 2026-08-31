package pe.edu.utp.Grupo06.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.utp.Grupo06.model.Usuario;
import pe.edu.utp.Grupo06.repository.UsuarioRepository;
import pe.edu.utp.Grupo06.service.IUsuarioService;

import java.util.List;

@Service
public class UsuarioServiceImpl implements IUsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

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
            existente.setPassword(usuarioActualizado.getPassword());
        }
        return usuarioRepository.save(existente);
    }

    @Override
    @Transactional
    public void cambiarEstado(Long id, Boolean activo) {
        Usuario usuario = buscarPorId(id);
        usuario.setActivo(activo);
        usuarioRepository.save(usuario);
    }
}