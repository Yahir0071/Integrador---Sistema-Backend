package pe.edu.utp.Grupo06.service;

import pe.edu.utp.Grupo06.model.Usuario;
import java.util.List;

public interface IUsuarioService {
    List<Usuario> listarTodos();
    List<Usuario> listarActivos();
    Usuario buscarPorId(Long id);
    Usuario buscarPorUsername(String username);
    Usuario registrar(Usuario usuario);
    Usuario actualizar(Long id, Usuario usuario);
    void cambiarEstado(Long id, Boolean activo);
}