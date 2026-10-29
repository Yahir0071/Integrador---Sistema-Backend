package pe.edu.utp.Grupo06.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.utp.Grupo06.dto.auth.RegistroClienteDTO;
import pe.edu.utp.Grupo06.model.Cliente;
import pe.edu.utp.Grupo06.repository.ClienteRepository;
import pe.edu.utp.Grupo06.service.IClienteService;
import pe.edu.utp.Grupo06.util.Validador;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class ClienteServiceImpl implements IClienteService {

    private static final int PASSWORD_MIN_LENGTH = 6;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private Validador validador;

    @Override
    @Transactional(readOnly = true)
    public List<Cliente> listarTodos() {
        return clienteRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Cliente buscarPorId(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado con ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public Cliente buscarPorUsername(String username) {
        return clienteRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado con username: " + username));
    }

    @Override
    @Transactional
    public Cliente registrar(RegistroClienteDTO datos) {
        if (clienteRepository.existsByUsername(datos.username())) {
            throw new RuntimeException("El nombre de usuario ya se encuentra registrado: " + datos.username());
        }
        if (clienteRepository.existsByEmail(datos.email().trim())) {
            throw new RuntimeException("El correo ya se encuentra registrado: " + datos.email().trim());
        }

        validarPasswordEnTextoPlano(datos.password());

        Cliente cliente = new Cliente();
        cliente.setUsername(datos.username());
        cliente.setPassword(passwordEncoder.encode(datos.password()));
        cliente.setNombreCompleto(datos.nombreCompleto().trim());
        cliente.setEmail(datos.email().trim());
        cliente.setTelefono(datos.telefono().trim());
        cliente.setActivo(true);

        validador.validar(cliente);
        return clienteRepository.save(cliente);
    }

    @Override
    @Transactional(readOnly = true)
    public Cliente validarCredenciales(String username, String password) {
        Cliente cliente = clienteRepository.findByUsername(username)
                .orElseThrow(() -> new BadCredentialsException("Usuario o contraseña incorrectos"));

        if (!Boolean.TRUE.equals(cliente.getActivo())) {
            throw new BadCredentialsException("Usuario o contraseña incorrectos");
        }

        if (!passwordEncoder.matches(password, cliente.getPassword())) {
            throw new BadCredentialsException("Usuario o contraseña incorrectos");
        }

        return cliente;
    }

    @Override
    @Transactional
    public void cambiarEstado(Long id, Boolean activo) {
        Cliente cliente = buscarPorId(id);
        cliente.setActivo(activo);
        clienteRepository.save(cliente);
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
