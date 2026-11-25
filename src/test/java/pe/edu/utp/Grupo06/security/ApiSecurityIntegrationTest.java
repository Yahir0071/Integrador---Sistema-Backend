package pe.edu.utp.Grupo06.security;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import pe.edu.utp.Grupo06.config.ApiSecurityConfig;
import pe.edu.utp.Grupo06.dto.auth.RegistroClienteDTO;
import pe.edu.utp.Grupo06.model.Cliente;
import pe.edu.utp.Grupo06.model.Empleado;
import pe.edu.utp.Grupo06.model.enums.RolNombre;
import pe.edu.utp.Grupo06.repository.*;
import pe.edu.utp.Grupo06.service.*;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.http.*;
import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Pruebas HTTP reales: filtros, JWT firmado, roles y persistencia PostgreSQL. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApiSecurityIntegrationTest {
    @Autowired Environment env;
    @Autowired ClienteRepository clientes;
    @Autowired EmpleadoRepository empleados;
    @Autowired IClienteService clienteService;
    @Autowired IEmpleadoService empleadoService;
    @Autowired PasswordEncoder passwords;
    @Autowired JwtEncoder encoder;
    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
    @Autowired ProductoRepository productos;
    @Autowired CategoriaRepository categorias;
    private final HttpClient http = HttpClient.newHttpClient();
    private final JsonMapper json = JsonMapper.builder().build();
    private final List<String> creados = new ArrayList<>();
    private static final String PASSWORD = "PruebaSegura2026!";

    @AfterEach
    void limpiar() {
        creados.forEach(nombre -> {
            clientes.findByUsername(nombre).ifPresent(clientes::delete);
            empleados.findByUsername(nombre).ifPresent(empleados::delete);
        });
    }

    private String nombre() {
        String nombre = "test_" + UUID.randomUUID().toString().replace("-", "");
        creados.add(nombre);
        return nombre;
    }

    private Cliente cuentaCliente() {
        String u = nombre();
        return clienteService.registrar(new RegistroClienteDTO(u, PASSWORD, "Prueba de seguridad", u + "@example.com", "999123456"));
    }

    private Empleado cuentaEmpleado(RolNombre rol) {
        Empleado e = new Empleado();
        e.setUsername(nombre()); e.setPassword(PASSWORD); e.setNombreCompleto("Prueba de seguridad");
        e.setEmail(e.getUsername() + "@example.com"); e.setTelefono("999123456");
        e.setRol(rol); e.setActivo(true);
        return empleadoService.registrar(e);
    }

    private HttpResponse<String> request(String method, String path, String body, String token) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + env.getProperty("local.server.port") + path))
                .header("Content-Type", "application/json");
        if (token != null) builder.header("Authorization", "Bearer " + token);
        return http.send(builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }

    private String tokenCliente(Cliente c) throws Exception {
        var response = request("POST", "/api/auth/login",
                json.writeValueAsString(Map.of("username", c.getUsername(), "password", PASSWORD)), null);
        assertEquals(200, response.statusCode(), response.body());
        return json.readTree(response.body()).path("accessToken").asText();
    }

    private String tokenEmpleado(Empleado e) throws Exception {
        var response = request("POST", "/api/personal/auth/login",
                json.writeValueAsString(Map.of("username", e.getUsername(), "password", PASSWORD)), null);
        assertEquals(200, response.statusCode(), response.body());
        return json.readTree(response.body()).path("accessToken").asText();
    }

    @Test void endpointsInternosExigenAutenticacion() throws Exception {
        for (String path : List.of("/api/usuarios", "/api/productos", "/api/ventas", "/api/reportes/inventario"))
            assertEquals(401, request("GET", path, null, null).statusCode(), path);
    }

    @Test void catalogoPublicoNoExponeInformacionInterna() throws Exception {
        var response=request("GET","/api/catalogo/productos",null,null);
        assertEquals(200,response.statusCode(),response.body());
        var lista=json.readTree(response.body());
        assertTrue(lista.isArray());
        assertTrue(lista.size()>0);
        assertFalse(response.body().contains("precioCompra"));
        assertFalse(response.body().contains("proveedor"));
        assertFalse(response.body().contains("stockMinimo"));
        assertEquals(200,request("GET","/api/catalogo/categorias",null,null).statusCode());
        long id=lista.path(0).path("id").asLong();
        var detail=request("GET","/api/catalogo/productos/"+id,null,null);
        assertEquals(200,detail.statusCode());
        assertEquals(id,json.readTree(detail.body()).path("id").asLong());
        assertEquals(404,request("GET","/api/catalogo/productos/9223372036854775807",null,null).statusCode());
        assertEquals(401,request("POST","/api/catalogo/productos","{}",null).statusCode());
    }

    @Test void productoInactivoNoSePublica() throws Exception {
        var p=new pe.edu.utp.Grupo06.model.Producto();
        p.setCodigo(nombre()); p.setNombre("Producto inactivo de prueba");
        p.setPrecioCompra(new java.math.BigDecimal("1.00")); p.setPrecioVenta(new java.math.BigDecimal("2.00"));
        p.setStockActual(10); p.setStockMinimo(0); p.setEstado(false);
        p.setCategoria(categorias.findAll().get(0));
        p.setUnidadMedida(pe.edu.utp.Grupo06.model.enums.UnidadMedida.UNIDAD);
        Long id=productos.saveAndFlush(p).getId();
        try {
            assertEquals(404,request("GET","/api/catalogo/productos/"+id,null,null).statusCode());
            var lista=json.readTree(request("GET","/api/catalogo/productos",null,null).body());
            for(var producto:lista) assertNotEquals(id.longValue(),producto.path("id").asLong());
        } finally { productos.deleteById(id); }
    }

    @Test void clienteNoAccedeAInformacionInterna() throws Exception {
        String token = tokenCliente(cuentaCliente());
        for (String path : List.of("/api/usuarios", "/api/productos", "/api/ventas", "/api/reportes/inventario"))
            assertEquals(403, request("GET", path, null, token).statusCode(), path);
        assertEquals(403, request("POST", "/api/usuarios", "{}", token).statusCode());
    }

    @Test void vendedorAccedeAPosPeroNoAdministracion() throws Exception {
        String token = tokenEmpleado(cuentaEmpleado(RolNombre.VENDEDOR));
        assertEquals(200, request("GET", "/api/productos", null, token).statusCode());
        assertEquals(403, request("GET", "/api/usuarios", null, token).statusCode());
        assertEquals(403, request("GET", "/api/compras", null, token).statusCode());
        assertEquals(403, request("POST", "/api/productos", "{}", token).statusCode());
    }

    @Test void administradorAccedeSinExponerHashes() throws Exception {
        String token = tokenEmpleado(cuentaEmpleado(RolNombre.ADMINISTRADOR));
        var response = request("GET", "/api/usuarios", null, token);
        assertEquals(200, response.statusCode(), response.body());
        assertFalse(response.body().contains("password"));
        assertFalse(response.body().contains("$2a$"));
    }

    @Test void registroIgnoraIntentosDeAsignarRolOId() throws Exception {
        String nombre = nombre();
        String datos = json.writeValueAsString(Map.of("username",nombre,"password",PASSWORD,
                "nombreCompleto","Cliente prueba","email",nombre+"@example.com","telefono","999123456",
                "rol","ADMINISTRADOR","activo",false,"id",1));
        var response = request("POST", "/api/auth/registro", datos, null);
        assertEquals(201, response.statusCode(), response.body());
        Cliente c = clientes.findByUsername(nombre).orElseThrow();
        assertTrue(c.getActivo());
        assertTrue(passwords.matches(PASSWORD, c.getPassword()));
        assertNotEquals(PASSWORD, c.getPassword());
        assertFalse(response.body().contains("password"));
        String token = json.readTree(response.body()).path("accessToken").asText();
        assertEquals(200, request("GET", "/api/auth/me", null, token).statusCode());
        assertEquals(403, request("GET", "/api/usuarios", null, token).statusCode());
    }

    @Test void rechazaDatosDeRegistroInvalidos() throws Exception {
        assertEquals(400, request("POST", "/api/auth/registro", "{}", null).statusCode());
        String nombre=nombre();
        var datos=Map.of("username",nombre,"password","corta","nombreCompleto","Prueba",
                "email","correo-invalido","telefono","999123456");
        assertEquals(400,request("POST","/api/auth/registro",json.writeValueAsString(datos),null).statusCode());
        assertTrue(clientes.findByUsername(nombre).isEmpty());
    }

    @Test void noPermiteRegistrarUsernameExistente() throws Exception {
        Cliente c=cuentaCliente();
        var datos=Map.of("username",c.getUsername(),"password",PASSWORD,"nombreCompleto","Otro usuario",
                "email","otro_"+c.getUsername()+"@example.com","telefono","999123456");
        var response=request("POST","/api/auth/registro",json.writeValueAsString(datos),null);
        assertEquals(400,response.statusCode());
    }

    @Test void perfilSiempreCorrespondeAlToken() throws Exception {
        Cliente cliente=cuentaCliente();
        Empleado otro=cuentaEmpleado(RolNombre.ADMINISTRADOR);
        var response=request("GET","/api/auth/me?id="+otro.getId(),null,tokenCliente(cliente));
        assertEquals(200,response.statusCode());
        assertEquals(cliente.getId().longValue(),json.readTree(response.body()).path("id").asLong());
    }

    @Test void auditoriaUsaUsuarioAutenticadoAunqueEnvienOtroId() throws Exception {
        Empleado vendedor=cuentaEmpleado(RolNombre.VENDEDOR);
        Empleado otro=cuentaEmpleado(RolNombre.ADMINISTRADOR);
        var p=new pe.edu.utp.Grupo06.model.Producto();
        p.setCodigo(nombre()); p.setNombre("Producto prueba auditoria");
        p.setPrecioCompra(new java.math.BigDecimal("1.00")); p.setPrecioVenta(new java.math.BigDecimal("2.00"));
        p.setStockActual(10); p.setStockMinimo(0); p.setCategoria(categorias.findAll().get(0));
        p.setUnidadMedida(pe.edu.utp.Grupo06.model.enums.UnidadMedida.UNIDAD);
        Long productoId=productos.saveAndFlush(p).getId();
        try {
            var datos=Map.of("productoId",productoId,"usuarioId",otro.getId(),"cantidad",1,
                    "motivo",pe.edu.utp.Grupo06.model.enums.MotivoMerma.ROTURA_DANO.toString(),
                    "observacion","Prueba de identidad");
            var response=request("POST","/api/mermas",json.writeValueAsString(datos),tokenEmpleado(vendedor));
            assertEquals(201,response.statusCode(),response.body());
            assertEquals(vendedor.getId().longValue(),json.readTree(response.body()).path("usuarioId").asLong());
            assertEquals(vendedor.getId(),jdbc.queryForObject(
                    "SELECT usuario_id FROM movimientos_inventario WHERE producto_id=?",Long.class,productoId));
        } finally {
            jdbc.update("DELETE FROM movimientos_inventario WHERE producto_id=?",productoId);
            jdbc.update("DELETE FROM mermas WHERE producto_id=?",productoId);
            jdbc.update("DELETE FROM alertas_reposicion WHERE producto_id=?",productoId);
            productos.deleteById(productoId);
        }
    }

    @Test void loginIncorrectoDevuelve401() throws Exception {
        Cliente c = cuentaCliente();
        assertEquals(401,request("POST","/api/auth/login",json.writeValueAsString(
                Map.of("username",c.getUsername(),"password","incorrecta")),null).statusCode());
        assertEquals(401,request("POST","/api/auth/login",json.writeValueAsString(
                Map.of("username",nombre(),"password",PASSWORD)),null).statusCode());
    }

    @Test void rechazaTokensManipuladosYVencidos() throws Exception {
        Cliente c=cuentaCliente();
        String valid=tokenCliente(c);
        String[] partes=valid.split("\\.");
        char primero=partes[2].charAt(0);
        String falso=partes[0]+"."+partes[1]+"."+(primero=='A'?'B':'A')+partes[2].substring(1);
        assertEquals(401,request("GET","/api/auth/me",null,falso).statusCode());
        Instant antes=Instant.now().minusSeconds(7200);
        var claims=JwtClaimsSet.builder().issuer(ApiSecurityConfig.ISSUER).subject(c.getId().toString()).claim("tipo","CLIENTE")
                .issuedAt(antes).expiresAt(antes.plusSeconds(60)).build();
        String vencido=encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),claims)).getTokenValue();
        assertEquals(401,request("GET","/api/auth/me",null,vencido).statusCode());
    }

    @Test void desactivarCuentaInvalidaTokenExistente() throws Exception {
        Cliente c=cuentaCliente(); String token=tokenCliente(c);
        clienteService.cambiarEstado(c.getId(),false);
        assertEquals(401,request("GET","/api/auth/me",null,token).statusCode());
        assertEquals(401,request("POST","/api/auth/login",json.writeValueAsString(
                Map.of("username",c.getUsername(),"password",PASSWORD)),null).statusCode());
    }

    @Test void cambiarRolActualizaPermisosDelToken() throws Exception {
        Empleado e=cuentaEmpleado(RolNombre.ADMINISTRADOR); String token=tokenEmpleado(e);
        e.setRol(RolNombre.VENDEDOR); empleados.saveAndFlush(e);
        assertEquals(403,request("GET","/api/usuarios",null,token).statusCode());
    }

    @Test void clienteNoPuedeIniciarSesionEnEscritorio() {
        Cliente c=cuentaCliente();
        assertThrows(org.springframework.security.authentication.BadCredentialsException.class,
                () -> empleadoService.validarCredenciales(c.getUsername(),PASSWORD));
        Empleado vendedor=cuentaEmpleado(RolNombre.VENDEDOR);
        assertEquals(vendedor.getId(),empleadoService.validarCredenciales(vendedor.getUsername(),PASSWORD).getId());
    }

    @Test void corsPermiteSoloFrontendConfigurado() throws Exception {
        String base="http://127.0.0.1:"+env.getProperty("local.server.port")+"/api/auth/login";
        for (String origin : List.of("http://localhost:4200","https://origen-no-permitido.example")) {
            var request=HttpRequest.newBuilder(URI.create(base)).header("Origin",origin)
                    .header("Access-Control-Request-Method","POST")
                    .header("Access-Control-Request-Headers","content-type").method("OPTIONS",HttpRequest.BodyPublishers.noBody()).build();
            var response=http.send(request,HttpResponse.BodyHandlers.ofString());
            if(origin.contains("localhost")) {
                assertEquals(200,response.statusCode());
                assertEquals(origin,response.headers().firstValue("Access-Control-Allow-Origin").orElse(""));
            } else assertEquals(403,response.statusCode());
        }
    }
}
