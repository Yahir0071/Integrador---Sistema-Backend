package pe.edu.utp.Grupo06;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import pe.edu.utp.Grupo06.controller.AuthController;
import pe.edu.utp.Grupo06.dto.auth.LoginRequestDTO;
import pe.edu.utp.Grupo06.dto.auth.LoginResponseDTO;
import pe.edu.utp.Grupo06.model.Cliente;
import pe.edu.utp.Grupo06.model.Empleado;
import pe.edu.utp.Grupo06.model.enums.RolNombre;
import pe.edu.utp.Grupo06.service.IClienteService;
import pe.edu.utp.Grupo06.service.IEmpleadoService;
import pe.edu.utp.Grupo06.service.TokenService;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:postgresql://aws-0-sa-east-1.pooler.supabase.com:5432/postgres?sslmode=require",
    "spring.datasource.username=postgres.qoupwzmfmsxtjasackkf",
    "spring.datasource.password=fZ7qKHBRTdetITPy"
})
class VerificacionLoginTest {

    @Autowired
    private IEmpleadoService empleadoService;

    @Autowired
    private IClienteService clienteService;

    @Autowired
    private AuthController authController;

    @Autowired
    private TokenService tokenService;

    @Test
    @DisplayName("1. App Escritorio (JavaFX): Login exitoso con empleado DiegoJ / DiegoJ")
    void testLoginEscritorioDiegoJ() {
        Empleado emp = empleadoService.validarCredenciales("DiegoJ", "DiegoJ");
        assertNotNull(emp, "El empleado debe autenticarse correctamente");
        assertEquals("DiegoJ", emp.getUsername());
        assertEquals(RolNombre.ADMINISTRADOR, emp.getRol());
        assertTrue(emp.getActivo(), "El empleado debe estar activo");
        System.out.println("✅ [TEST ESCRITORIO] Empleado DiegoJ autenticado con éxito: ID=" + emp.getId() + ", Rol=" + emp.getRol() + ", Nombre=" + emp.getNombreCompleto());
    }

    @Test
    @DisplayName("2. Web Cliente (E-Commerce): Login exitoso con cliente Joel123 / Joel12345678")
    void testLoginWebClienteJoel123() {
        Cliente cli = clienteService.validarCredenciales("Joel123", "Joel12345678");
        assertNotNull(cli, "El cliente debe autenticarse correctamente");
        assertEquals("Joel123", cli.getUsername());
        assertEquals("joel@gmail.com", cli.getEmail());
        assertTrue(cli.getActivo(), "El cliente debe estar activo");

        // Simular llamada al endpoint REST /api/auth/login de la web
        LoginResponseDTO response = authController.login(new LoginRequestDTO("Joel123", "Joel12345678")).getBody();
        assertNotNull(response);
        assertEquals(RolNombre.CLIENTE, response.getRol());
        assertNotNull(response.getAccessToken(), "Debe emitirse el token JWT");
        System.out.println("✅ [TEST WEB CLIENTE] Cliente Joel123 autenticado con éxito: ID=" + response.getId() + ", Token generado=" + response.getAccessToken().substring(0, 25) + "...");
    }

    @Test
    @DisplayName("3. Segregación: Cliente Joel123 NO puede ingresar al sistema de escritorio")
    void testClienteNoPuedeEntrarEnEscritorio() {
        assertThrows(BadCredentialsException.class, () -> {
            empleadoService.validarCredenciales("Joel123", "Joel12345678");
        }, "Un cliente no debe existir en la tabla de empleados");
        System.out.println("✅ [TEST SEGREGACIÓN] Cliente Joel123 rechazado correctamente en el login de empleados");
    }

    @Test
    @DisplayName("4. Segregación: Empleado DiegoJ es rechazado en el login web de clientes con mensaje explicativo")
    void testEmpleadoRechazadoEnWebCliente() {
        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () -> {
            authController.login(new LoginRequestDTO("DiegoJ", "DiegoJ"));
        });
        assertTrue(ex.getMessage().contains("personal de la bodega"), "Debe mostrar el mensaje de cuenta de personal");
        System.out.println("✅ [TEST SEGREGACIÓN] Empleado DiegoJ bloqueado en /api/auth/login con mensaje: \"" + ex.getMessage() + "\"");
    }
}
