package pe.edu.utp.Grupo06.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.utp.Grupo06.model.Empleado;
import pe.edu.utp.Grupo06.model.enums.RolNombre;
import pe.edu.utp.Grupo06.repository.EmpleadoRepository;
import pe.edu.utp.Grupo06.service.IEmpleadoService;
import pe.edu.utp.Grupo06.util.Validador;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class EmpleadoServiceImpl implements IEmpleadoService {

    private static final int PASSWORD_MIN_LENGTH = 6;

    @Autowired
    private EmpleadoRepository empleadoRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private Validador validador;

    @Override
    @Transactional(readOnly = true)
    public List<Empleado> listarTodos() {
        return empleadoRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Empleado> listarActivos() {
        return empleadoRepository.findByActivoTrue();
    }

    @Override
    @Transactional(readOnly = true)
    public Empleado buscarPorId(Long id) {
        return empleadoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Empleado no encontrado con ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public Empleado buscarPorUsername(String username) {
        return empleadoRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Empleado no encontrado con username: " + username));
    }

    @Override
    @Transactional
    public Empleado registrar(Empleado empleado) {
        if (empleadoRepository.existsByUsername(empleado.getUsername())) {
            throw new RuntimeException("El nombre de usuario ya se encuentra registrado: " + empleado.getUsername());
        }
        if (empleado.getEmail() != null && !empleado.getEmail().isBlank()) {
            if (empleadoRepository.existsByEmail(empleado.getEmail())) {
                throw new RuntimeException("El correo ya se encuentra registrado: " + empleado.getEmail());
            }
        }

        validarPasswordEnTextoPlano(empleado.getPassword());
        empleado.setPassword(passwordEncoder.encode(empleado.getPassword()));

        if (empleado.getActivo() == null) {
            empleado.setActivo(true);
        }

        validador.validar(empleado);
        return empleadoRepository.save(empleado);
    }

    @Override
    @Transactional
    public Empleado actualizar(Long id, Empleado empleado) {
        Empleado existente = buscarPorId(id);

        if (!existente.getUsername().equals(empleado.getUsername())) {
            if (empleadoRepository.existsByUsername(empleado.getUsername())) {
                throw new RuntimeException("El nombre de usuario ya está en uso: " + empleado.getUsername());
            }
            existente.setUsername(empleado.getUsername());
        }

        if (empleado.getEmail() != null && !empleado.getEmail().isBlank()) {
            if (!empleado.getEmail().equals(existente.getEmail())) {
                if (empleadoRepository.existsByEmail(empleado.getEmail())) {
                    throw new RuntimeException("El correo ya está en uso: " + empleado.getEmail());
                }
            }
            existente.setEmail(empleado.getEmail());
        } else {
            existente.setEmail(null);
        }

        existente.setNombreCompleto(empleado.getNombreCompleto());
        existente.setTelefono(empleado.getTelefono());
        existente.setRol(empleado.getRol());

        if (empleado.getPassword() != null && !empleado.getPassword().isBlank()) {
            validarPasswordEnTextoPlano(empleado.getPassword());
            existente.setPassword(passwordEncoder.encode(empleado.getPassword()));
        }

        validador.validar(existente);
        return empleadoRepository.save(existente);
    }

    @Override
    @Transactional
    public void cambiarEstado(Long id, Boolean activo) {
        Empleado empleado = buscarPorId(id);
        if (Boolean.FALSE.equals(activo)) {
            if (empleado.getRol() == RolNombre.ADMINISTRADOR) {
                long adminsActivos = empleadoRepository.findByRol(RolNombre.ADMINISTRADOR).stream()
                        .filter(e -> Boolean.TRUE.equals(e.getActivo()))
                        .count();
                if (adminsActivos <= 1) {
                    throw new RuntimeException("No se puede desactivar al único Administrador activo del sistema. Debe existir al menos un administrador habilitado.");
                }
            }
        }
        empleado.setActivo(activo);
        empleadoRepository.save(empleado);
    }

    @Override
    @Transactional
    public Empleado actualizarPerfil(Long id, String nombreCompleto, String email, String telefono, String passwordActual, String nuevaPassword) {
        Empleado empleado = buscarPorId(id);

        if (nombreCompleto != null && !nombreCompleto.isBlank()) {
            empleado.setNombreCompleto(nombreCompleto.trim());
        }
        if (email != null && !email.isBlank()) {
            empleado.setEmail(email.trim());
        }
        if (telefono != null) {
            empleado.setTelefono(telefono.trim());
        }

        if (nuevaPassword != null && !nuevaPassword.isBlank()) {
            if (passwordActual == null || passwordActual.isBlank()) {
                throw new RuntimeException("Debe ingresar su contraseña actual para establecer una nueva.");
            }
            if (!passwordEncoder.matches(passwordActual, empleado.getPassword())) {
                throw new RuntimeException("La contraseña actual ingresada es incorrecta.");
            }
            validarPasswordEnTextoPlano(nuevaPassword);
            empleado.setPassword(passwordEncoder.encode(nuevaPassword));
        }

        validador.validar(empleado);
        return empleadoRepository.save(empleado);
    }

    @Override
    @Transactional(readOnly = true)
    public Empleado validarCredenciales(String username, String password) {
        Empleado empleado = empleadoRepository.findByUsername(username)
                .orElseThrow(() -> new BadCredentialsException("Usuario o contraseña incorrectos"));

        if (!Boolean.TRUE.equals(empleado.getActivo())) {
            throw new BadCredentialsException("Usuario o contraseña incorrectos");
        }

        if (!passwordEncoder.matches(password, empleado.getPassword())) {
            throw new BadCredentialsException("Usuario o contraseña incorrectos");
        }

        return empleado;
    }

    private void validarPasswordEnTextoPlano(String password) {
        if (password == null || password.isBlank()) {
            throw new RuntimeException("La contraseña es obligatoria");
        }
        if (password.length() < PASSWORD_MIN_LENGTH) {
            throw new RuntimeException("La contraseña debe tener al menos " + PASSWORD_MIN_LENGTH + " caracteres");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("La contraseña no puede superar 72 bytes en UTF-8");
        }
    }
}
