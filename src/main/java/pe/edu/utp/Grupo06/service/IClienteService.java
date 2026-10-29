package pe.edu.utp.Grupo06.service;

import pe.edu.utp.Grupo06.dto.auth.RegistroClienteDTO;
import pe.edu.utp.Grupo06.model.Cliente;
import java.util.List;

public interface IClienteService {
    List<Cliente> listarTodos();
    Cliente buscarPorId(Long id);
    Cliente buscarPorUsername(String username);
    Cliente registrar(RegistroClienteDTO datos);
    Cliente validarCredenciales(String username, String password);
    void cambiarEstado(Long id, Boolean activo);
}
