package pe.edu.utp.Grupo06.service;

import pe.edu.utp.Grupo06.model.Empleado;
import java.util.List;

public interface IEmpleadoService {
    List<Empleado> listarTodos();
    List<Empleado> listarActivos();
    Empleado buscarPorId(Long id);
    Empleado buscarPorUsername(String username);
    Empleado registrar(Empleado empleado);
    Empleado actualizar(Long id, Empleado empleado);
    void cambiarEstado(Long id, Boolean activo);
    Empleado validarCredenciales(String username, String password);
    Empleado actualizarPerfil(Long id, String nombreCompleto, String email, String telefono, String passwordActual, String nuevaPassword);
}
