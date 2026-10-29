package pe.edu.utp.Grupo06;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "spring.datasource.url=jdbc:postgresql://aws-0-sa-east-1.pooler.supabase.com:5432/postgres?sslmode=require",
        "spring.datasource.username=postgres.qoupwzmfmsxtjasackkf",
        "spring.datasource.password=fZ7qKHBRTdetITPy"
    }
)
class HttpEndToEndTest {

    @Autowired
    private Environment env;

    private final HttpClient http = HttpClient.newHttpClient();
    private final JsonMapper json = JsonMapper.builder().build();

    private String url(String path) {
        return "http://127.0.0.1:" + env.getProperty("local.server.port") + path;
    }

    @Test
    @DisplayName("HTTP E2E: Login web con Joel123 y consulta a /api/auth/me")
    void testFlujoWebClienteHttp() throws Exception {
        // 1. POST /api/auth/login con Joel123
        String body = json.writeValueAsString(Map.of("username", "Joel123", "password", "Joel12345678"));
        HttpRequest loginReq = HttpRequest.newBuilder(URI.create(url("/api/auth/login")))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> loginRes = http.send(loginReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, loginRes.statusCode(), "El login de Joel123 debe ser 200 OK: " + loginRes.body());

        Map map = json.readValue(loginRes.body(), Map.class);
        assertEquals("CLIENTE", map.get("rol"));
        String token = (String) map.get("accessToken");
        assertNotNull(token, "El token no puede ser nulo");
        System.out.println("🌐 [HTTP E2E] Login /api/auth/login exitoso para Joel123. Token=" + token.substring(0, 25) + "...");

        // 2. GET /api/auth/me con el token
        HttpRequest meReq = HttpRequest.newBuilder(URI.create(url("/api/auth/me")))
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();

        HttpResponse<String> meRes = http.send(meReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, meRes.statusCode(), "Consulta /api/auth/me debe ser 200 OK");
        Map meMap = json.readValue(meRes.body(), Map.class);
        assertEquals("Joel123", meMap.get("username"));
        assertEquals("joel@gmail.com", meMap.get("email"));
        assertEquals("CLIENTE", meMap.get("rol"));
        System.out.println("🌐 [HTTP E2E] Perfil /api/auth/me obtenido para Joel123: " + meRes.body());
    }

    @Test
    @DisplayName("HTTP E2E: Personal DiegoJ bloqueado en /api/auth/login con 403 Forbidden")
    void testEmpleadoBloqueadoEnLoginWebHttp() throws Exception {
        String body = json.writeValueAsString(Map.of("username", "DiegoJ", "password", "DiegoJ"));
        HttpRequest loginReq = HttpRequest.newBuilder(URI.create(url("/api/auth/login")))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> res = http.send(loginReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(403, res.statusCode(), "Debe responder 403 Forbidden: " + res.body());
        System.out.println("🌐 [HTTP E2E] Empleado DiegoJ rechazado con HTTP 403 en /api/auth/login: " + res.body());
    }

    @Test
    @DisplayName("HTTP E2E: Login personal staff con DiegoJ en /api/personal/auth/login y consulta a /api/personal/auth/me")
    void testFlujoPersonalStaffHttp() throws Exception {
        // 1. POST /api/personal/auth/login con DiegoJ
        String body = json.writeValueAsString(Map.of("username", "DiegoJ", "password", "DiegoJ"));
        HttpRequest loginReq = HttpRequest.newBuilder(URI.create(url("/api/personal/auth/login")))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> loginRes = http.send(loginReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, loginRes.statusCode(), "El login de DiegoJ en personal debe ser 200 OK: " + loginRes.body());

        Map map = json.readValue(loginRes.body(), Map.class);
        assertEquals("ADMINISTRADOR", map.get("rol"));
        String token = (String) map.get("accessToken");
        assertNotNull(token);
        System.out.println("🌐 [HTTP E2E] Login /api/personal/auth/login exitoso para DiegoJ. Rol=" + map.get("rol"));

        // 2. GET /api/personal/auth/me con el token
        HttpRequest meReq = HttpRequest.newBuilder(URI.create(url("/api/personal/auth/me")))
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();

        HttpResponse<String> meRes = http.send(meReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, meRes.statusCode());
        Map meMap = json.readValue(meRes.body(), Map.class);
        assertEquals("DiegoJ", meMap.get("username"));
        assertEquals("ADMINISTRADOR", meMap.get("rol"));
        assertEquals("Diego Avila", meMap.get("nombreCompleto"));
        System.out.println("🌐 [HTTP E2E] Perfil /api/personal/auth/me obtenido para DiegoJ: " + meRes.body());
    }
}
