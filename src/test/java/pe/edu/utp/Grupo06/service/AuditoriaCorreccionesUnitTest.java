package pe.edu.utp.Grupo06.service;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import pe.edu.utp.Grupo06.controller.ReporteController;
import pe.edu.utp.Grupo06.dto.auth.LoginRequestDTO;
import pe.edu.utp.Grupo06.dto.auth.RegistroClienteDTO;
import pe.edu.utp.Grupo06.dto.compra.CompraRequestDTO;
import pe.edu.utp.Grupo06.dto.compra.DetalleCompraRequestDTO;
import pe.edu.utp.Grupo06.dto.pedido.CrearPedido;
import pe.edu.utp.Grupo06.dto.pedido.PedidoDTO;
import pe.edu.utp.Grupo06.dto.reporte.ResumenVentasDTO;
import pe.edu.utp.Grupo06.dto.usuario.UsuarioRequestDTO;
import pe.edu.utp.Grupo06.dto.venta.DetalleVentaRequestDTO;
import pe.edu.utp.Grupo06.dto.venta.PagoRequestDTO;
import pe.edu.utp.Grupo06.dto.venta.VentaRequestDTO;
import pe.edu.utp.Grupo06.exception.ErrorResponse;
import pe.edu.utp.Grupo06.exception.GlobalExceptionHandler;
import pe.edu.utp.Grupo06.model.*;
import pe.edu.utp.Grupo06.model.enums.EstadoPedido;
import pe.edu.utp.Grupo06.model.enums.EstadoVenta;
import pe.edu.utp.Grupo06.model.enums.RolNombre;
import pe.edu.utp.Grupo06.repository.*;
import pe.edu.utp.Grupo06.service.impl.ExportacionServiceImpl;
import pe.edu.utp.Grupo06.service.impl.ReporteServiceImpl;

import javax.xml.parsers.DocumentBuilderFactory;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuditoriaCorreccionesUnitTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void registroClienteNormalizaYValidaNombreTelefonoYUtf8Bytes() {
        var valido = new RegistroClienteDTO(
                "juan.perez",
                "Segura123!",
                "  Juan   Carlos   Pérez  ",
                "juan@example.com",
                " 987654321 "
        );
        assertEquals("Juan Carlos Pérez", valido.nombreCompleto());
        assertEquals("987654321", valido.telefono());
        assertTrue(validator.validate(valido).isEmpty());

        var invalido = new RegistroClienteDTO(
                "juan.perez",
                "á".repeat(40), // 40 chars pero 80 bytes UTF-8 (>72)
                "Juan 123",
                "juan@example.com",
                "123456789"
        );
        Set<ConstraintViolation<RegistroClienteDTO>> violaciones = validator.validate(invalido);
        var campos = violaciones.stream().map(v -> v.getPropertyPath().toString()).toList();
        assertTrue(campos.contains("password"));
        assertTrue(campos.contains("nombreCompleto"));
        assertTrue(campos.contains("telefono"));
    }

    @Test
    void usuarioYLoginProtegenPasswordUtf8YValidanTelefonoYNombre() {
        var loginLargo = new LoginRequestDTO("admin", "€".repeat(30)); // 30 chars = 90 bytes UTF-8
        var loginCampos = validator.validate(loginLargo).stream()
                .map(v -> v.getPropertyPath().toString()).toList();
        assertTrue(loginCampos.contains("password"));

        var usuario = new UsuarioRequestDTO(
                "cajero1",
                "Clave123",
                "  María   José  Ñañez ",
                "maria@example.com",
                " 912345678 ",
                RolNombre.VENDEDOR
        );
        assertEquals("María José Ñañez", usuario.getNombreCompletoNormalizado());
        assertEquals("912345678", usuario.getTelefonoNormalizado());
        assertTrue(validator.validate(usuario).isEmpty());

        usuario.setTelefono("812345678");
        assertFalse(validator.validate(usuario).isEmpty());
    }

    @Test
    void listasDeDtosRechazanElementosNulos() {
        var pedidoConNull = new CrearPedido(
                UUID.randomUUID().toString(),
                Arrays.asList((CrearPedido.Item) null)
        );
        assertFalse(validator.validate(pedidoConNull).isEmpty());

        var compraConNull = new CompraRequestDTO(
                "F001-1", "FACTURA", 1L, null,
                Arrays.asList((DetalleCompraRequestDTO) null)
        );
        assertFalse(validator.validate(compraConNull).isEmpty());

        var ventaConNull = new VentaRequestDTO(
                "B001-1", null,
                Arrays.asList((DetalleVentaRequestDTO) null),
                Arrays.asList((PagoRequestDTO) null)
        );
        assertEquals(2, validator.validate(ventaConNull).size());
    }

    @Test
    void globalExceptionHandlerOcultaInternosYDevuelveMapaEnValidacion() throws Exception {
        var handler = new GlobalExceptionHandler();

        var resp500 = handler.manejarRuntimeException(new NullPointerException("detalles internos sensibles"));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resp500.getStatusCode());
        assertNotNull(resp500.getBody());
        assertEquals("Ocurrió un error inesperado al procesar la solicitud", resp500.getBody().getMensaje());
        assertFalse(resp500.getBody().getMensaje().contains("NullPointerException"));
        assertFalse(resp500.getBody().getMensaje().contains("sensibles"));

        var resp404 = handler.manejarIllegalArgument(new IllegalArgumentException("Producto no encontrado"));
        assertEquals(HttpStatus.NOT_FOUND, resp404.getStatusCode());

        var resp400 = handler.manejarIllegalArgument(new IllegalArgumentException("La fecha de inicio no puede ser posterior a la fecha de fin"));
        assertEquals(HttpStatus.BAD_REQUEST, resp400.getStatusCode());

        var resp409 = handler.manejarIllegalState(new IllegalStateException("La venta ya está anulada"));
        assertEquals(HttpStatus.CONFLICT, resp409.getStatusCode());

        var binding = new BeanPropertyBindingResult(new LoginRequestDTO(), "loginRequestDTO");
        binding.addError(new FieldError("loginRequestDTO", "username", "El nombre de usuario es obligatorio"));
        Method m = this.getClass().getDeclaredMethod("helperValidacionDummy", LoginRequestDTO.class);
        var exValid = new MethodArgumentNotValidException(new MethodParameter(m, 0), binding);
        var respValid = handler.manejarMethodArgumentNotValid(exValid);
        assertEquals(HttpStatus.BAD_REQUEST, respValid.getStatusCode());
        ErrorResponse body = respValid.getBody();
        assertNotNull(body);
        assertInstanceOf(Map.class, body.getDetalles());
        @SuppressWarnings("unchecked")
        Map<String, String> detalles = (Map<String, String>) body.getDetalles();
        assertEquals("El nombre de usuario es obligatorio", detalles.get("username"));
    }

    @SuppressWarnings("unused")
    private void helperValidacionDummy(LoginRequestDTO dto) {}

    @Test
    void pedidoServiceUsaConsultasBatchEnMiosYTodos() {
        PedidoRepository pedidoRepo = mock(PedidoRepository.class);
        ProductoRepository prodRepo = mock(ProductoRepository.class);
        ClienteRepository cliRepo = mock(ClienteRepository.class);
        EmpleadoRepository empRepo = mock(EmpleadoRepository.class);
        ComprobantePedidoRepository compRepo = mock(ComprobantePedidoRepository.class);
        EventoPedidoRepository evtRepo = mock(EventoPedidoRepository.class);
        VentaRepository ventaRepo = mock(VentaRepository.class);
        IMovimientoInventarioService movSvc = mock(IMovimientoInventarioService.class);
        TicketService ticketSvc = mock(TicketService.class);
        TiendaConfig tienda = new TiendaConfig(true, "Nova", "999999999", "Dir", "Hor", "Url");

        PedidoService service = new PedidoService(
                pedidoRepo, prodRepo, cliRepo, empRepo, compRepo, evtRepo, ventaRepo, movSvc, ticketSvc, tienda
        );

        Cliente cliente = new Cliente();
        cliente.setId(10L);
        cliente.setActivo(true);
        cliente.setNombreCompleto("Cliente Web");

        Pedido p1 = new Pedido();
        p1.setId(101L);
        p1.setNumero("NOVA-1");
        p1.setEstado(EstadoPedido.PENDIENTE_VERIFICACION);
        p1.setCliente(cliente);
        p1.setCreado(LocalDateTime.now());
        p1.setVence(LocalDateTime.now().plusMinutes(10));
        p1.setTotal(new BigDecimal("15.00"));

        Pedido p2 = new Pedido();
        p2.setId(102L);
        p2.setNumero("NOVA-2");
        p2.setEstado(EstadoPedido.PENDIENTE_PAGO);
        p2.setCliente(cliente);
        p2.setCreado(LocalDateTime.now());
        p2.setVence(LocalDateTime.now().plusMinutes(10));
        p2.setTotal(new BigDecimal("20.00"));

        EventoPedido ev1 = new EventoPedido();
        ev1.setPedidoId(101L);
        ev1.setFecha(LocalDateTime.now());
        ev1.setEstado("PENDIENTE_VERIFICACION");
        ev1.setMensaje("Comprobante adjunto");

        when(cliRepo.findById(10L)).thenReturn(Optional.of(cliente));
        when(pedidoRepo.findTop100ByClienteIdOrderByCreadoDesc(10L)).thenReturn(List.of(p1, p2));
        when(compRepo.findPedidoIdsByPedidoIdIn(List.of(101L, 102L))).thenReturn(Set.of(101L));
        when(evtRepo.findByPedidoIdInOrderByFechaAscIdAsc(List.of(101L, 102L))).thenReturn(List.of(ev1));

        List<PedidoDTO> dtos = service.mios(10L);
        assertEquals(2, dtos.size());
        assertTrue(dtos.get(0).tieneComprobante());
        assertFalse(dtos.get(1).tieneComprobante());
        assertEquals(1, dtos.get(0).eventos().size());
        assertEquals(0, dtos.get(1).eventos().size());

        verify(compRepo, times(1)).findPedidoIdsByPedidoIdIn(List.of(101L, 102L));
        verify(evtRepo, times(1)).findByPedidoIdInOrderByFechaAscIdAsc(List.of(101L, 102L));
        verify(compRepo, never()).existsById(any());
        verify(evtRepo, never()).findByPedidoIdOrderByFechaAscIdAsc(any());
    }

    @Test
    void reporteServiceExcluyeAnuladaYDemoYValidaRangoInvertido() throws Exception {
        ProductoRepository prodRepo = mock(ProductoRepository.class);
        AlertaReposicionRepository alertaRepo = mock(AlertaReposicionRepository.class);
        VentaRepository ventaRepo = mock(VentaRepository.class);
        DetalleVentaRepository detRepo = mock(DetalleVentaRepository.class);

        ReporteServiceImpl service = new ReporteServiceImpl();
        inyectar(service, "productoRepository", prodRepo);
        inyectar(service, "alertaRepository", alertaRepo);
        inyectar(service, "ventaRepository", ventaRepo);
        inyectar(service, "detalleVentaRepository", detRepo);

        LocalDateTime inicio = LocalDateTime.of(2026, 11, 25, 0, 0);
        LocalDateTime fin = LocalDateTime.of(2026, 11, 26, 0, 0);

        assertThrows(IllegalArgumentException.class,
                () -> service.obtenerResumenVentasPorPeriodo(fin, inicio));

        Venta vEmitida = new Venta();
        vEmitida.setEstado(EstadoVenta.EMITIDA);
        vEmitida.setDemo(false);
        vEmitida.setTotal(new BigDecimal("50.00"));

        Venta vAnulada = new Venta();
        vAnulada.setEstado(EstadoVenta.ANULADA);
        vAnulada.setDemo(false);
        vAnulada.setTotal(new BigDecimal("100.00"));

        Venta vDemo = new Venta();
        vDemo.setEstado(EstadoVenta.EMITIDA);
        vDemo.setDemo(true);
        vDemo.setTotal(new BigDecimal("200.00"));

        when(ventaRepo.findByRangoSemiabiertoOrderByFechaVentaDesc(inicio, fin))
                .thenReturn(List.of(vEmitida, vAnulada, vDemo));
        when(detRepo.sumUnidadesVendidasEntreFechas(inicio, fin)).thenReturn(7L);

        ResumenVentasDTO resumen = service.obtenerResumenVentasPorPeriodo(inicio, fin);
        assertEquals(1L, resumen.getCantidadVentasEmitidas());
        assertEquals(1L, resumen.getCantidadVentasAnuladas());
        assertEquals(0, new BigDecimal("50.00").compareTo(resumen.getTotalRecaudado()));
        assertEquals(7L, resumen.getUnidadesVendidas());

        ReporteController controller = new ReporteController();
        inyectar(controller, "reporteService", service);
        assertThrows(IllegalArgumentException.class,
                () -> controller.obtenerResumenVentas(fin, inicio));
        assertThrows(IllegalArgumentException.class,
                () -> controller.obtenerProductosMayorRotacion(fin, inicio, null));
    }

    @Test
    void exportacionVentasExcelExcluyeAnuladaYDemoDeTotales(@TempDir Path tmp) throws Exception {
        Venta emitida = new Venta();
        emitida.setNumeroTicket("B001-00000001");
        emitida.setEstado(EstadoVenta.EMITIDA);
        emitida.setDemo(false);
        emitida.setTotal(new BigDecimal("118.00"));
        emitida.setSubtotal(new BigDecimal("100.00"));
        emitida.setIgv(new BigDecimal("18.00"));
        emitida.setFechaVenta(LocalDateTime.of(2026, 11, 25, 10, 0));

        Venta anulada = new Venta();
        anulada.setNumeroTicket("B001-00000002");
        anulada.setEstado(EstadoVenta.ANULADA);
        anulada.setDemo(false);
        anulada.setTotal(new BigDecimal("500.00"));
        anulada.setSubtotal(new BigDecimal("423.73"));
        anulada.setIgv(new BigDecimal("76.27"));
        anulada.setFechaVenta(LocalDateTime.of(2026, 11, 25, 11, 0));

        Venta demo = new Venta();
        demo.setNumeroTicket("D001-00000001");
        demo.setEstado(EstadoVenta.EMITIDA);
        demo.setDemo(true);
        demo.setTotal(new BigDecimal("999.00"));
        demo.setSubtotal(new BigDecimal("846.61"));
        demo.setIgv(new BigDecimal("152.39"));
        demo.setFechaVenta(LocalDateTime.of(2026, 11, 25, 12, 0));

        var destino = tmp.resolve("ventas-filtradas.xlsx").toFile();
        new ExportacionServiceImpl().exportarVentasExcel(destino, List.of(emitida, anulada, demo), null, null);

        try (var wb = new XSSFWorkbook(destino)) {
            var sheet = wb.getSheet("Reporte de Ventas");
            assertNotNull(sheet);
            // Cabecera en fila 4, emitida en fila 5, anulada en fila 6 (demo omitida), totales en fila 7
            var totRow = sheet.getRow(7);
            assertNotNull(totRow);
            assertEquals("TOTALES (1 Ventas emitidas)", totRow.getCell(0).getStringCellValue());
            assertEquals(100.00, totRow.getCell(3).getNumericCellValue(), 0.01);
            assertEquals(18.00, totRow.getCell(4).getNumericCellValue(), 0.01);
            assertEquals(118.00, totRow.getCell(5).getNumericCellValue(), 0.01);
        }
    }

    @Test
    void fxmlModificadosSonXmlValidosYSusIdsYAccionesExistenEnControladores() throws Exception {
        List<String> archivos = List.of(
                "/fxml/reportes.fxml",
                "/fxml/productos.fxml",
                "/fxml/compras.fxml",
                "/fxml/admin_panel.fxml",
                "/fxml/ventas_pos.fxml",
                "/fxml/pedidos_web.fxml"
        );
        var dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);

        for (String ruta : archivos) {
            try (var is = getClass().getResourceAsStream(ruta)) {
                assertNotNull(is, "Falta recurso " + ruta);
                var doc = dbf.newDocumentBuilder().parse(is);
                var root = doc.getDocumentElement();
                String ctrlName = root.getAttributeNS("http://javafx.com/fxml/1", "controller");
                assertFalse(ctrlName.isBlank(), "Sin fx:controller en " + ruta);
                Class<?> ctrlClass = Class.forName(ctrlName);

                Set<String> fxIdsEnXml = new HashSet<>();
                var allNodes = doc.getElementsByTagName("*");
                for (int i = 0; i < allNodes.getLength(); i++) {
                    var el = (org.w3c.dom.Element) allNodes.item(i);
                    String fxId = el.getAttributeNS("http://javafx.com/fxml/1", "id");
                    if (!fxId.isBlank()) {
                        fxIdsEnXml.add(fxId);
                    }
                    String onAction = el.getAttribute("onAction");
                    if (!onAction.isBlank() && onAction.startsWith("#")) {
                        String methodName = onAction.substring(1);
                        assertNotNull(buscarMetodo(ctrlClass, methodName),
                                "Método '" + methodName + "' no existe en " + ctrlClass.getSimpleName());
                    }
                }
                for (Field f : ctrlClass.getDeclaredFields()) {
                    if (f.isAnnotationPresent(javafx.fxml.FXML.class)) {
                        assertTrue(fxIdsEnXml.contains(f.getName()),
                                "Campo @FXML '" + f.getName() + "' de " + ctrlClass.getSimpleName() + " falta en " + ruta);
                    }
                }
            }
        }
    }

    private static Field buscarCampo(Class<?> cls, String name) {
        for (Class<?> c = cls; c != null; c = c.getSuperclass()) {
            try { return c.getDeclaredField(name); } catch (NoSuchFieldException ignored) {}
        }
        return null;
    }

    private static Method buscarMetodo(Class<?> cls, String name) {
        for (Class<?> c = cls; c != null; c = c.getSuperclass()) {
            for (Method m : c.getDeclaredMethods()) {
                if (m.getName().equals(name)) return m;
            }
        }
        return null;
    }

    private static void inyectar(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }
}
