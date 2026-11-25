package pe.edu.utp.Grupo06.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.utp.Grupo06.model.Categoria;
import pe.edu.utp.Grupo06.model.Empleado;
import pe.edu.utp.Grupo06.model.Merma;
import pe.edu.utp.Grupo06.model.MovimientoInventario;
import pe.edu.utp.Grupo06.model.Producto;
import pe.edu.utp.Grupo06.model.enums.MotivoMerma;
import pe.edu.utp.Grupo06.model.enums.RolNombre;
import pe.edu.utp.Grupo06.model.enums.TipoMovimiento;
import pe.edu.utp.Grupo06.model.enums.UnidadMedida;
import pe.edu.utp.Grupo06.repository.CategoriaRepository;
import pe.edu.utp.Grupo06.repository.EmpleadoRepository;
import pe.edu.utp.Grupo06.repository.MovimientoInventarioRepository;
import pe.edu.utp.Grupo06.repository.ProductoRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class MermaIntegrationTest {

    @Autowired
    private IMermaService mermaService;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private EmpleadoRepository empleadoRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private MovimientoInventarioRepository movimientoRepository;

    private Empleado obtenerOCrearEmpleado() {
        return empleadoRepository.findAll().stream()
                .filter(e -> Boolean.TRUE.equals(e.getActivo()))
                .findFirst()
                .orElseGet(() -> {
                    Empleado e = new Empleado();
                    e.setUsername("emp_merma_" + System.currentTimeMillis());
                    e.setPassword("hash-prueba");
                    e.setNombreCompleto("Empleado Merma Test");
                    e.setRol(RolNombre.ADMINISTRADOR);
                    e.setActivo(true);
                    return empleadoRepository.saveAndFlush(e);
                });
    }

    @Test
    @DisplayName("Debe registrar merma, calcular pérdida valorizada, reducir stock y auditar en Kardex")
    void testRegistrarMermaExitosa() {
        // Preparar empleado
        Empleado usuario = obtenerOCrearEmpleado();
        assertNotNull(usuario, "Debe existir al menos un empleado activo");

        // Preparar categoría
        Categoria categoria = categoriaRepository.findAll().stream().findFirst().orElseGet(() -> {
            Categoria c = new Categoria();
            c.setNombre("Lácteos Test");
            c.setDescripcion("Cat prueba");
            return categoriaRepository.save(c);
        });

        // Crear producto de prueba
        Producto producto = new Producto();
        producto.setCodigo("PRD-TEST-MERMA-" + System.currentTimeMillis());
        producto.setNombre("Yogurt Fresa 1L");
        producto.setMarca("Gloria");
        producto.setCategoria(categoria);
        producto.setPrecioCompra(new BigDecimal("5.50"));
        producto.setPrecioVenta(new BigDecimal("7.50"));
        producto.setStockActual(20);
        producto.setStockMinimo(5);
        producto.setUnidadMedida(UnidadMedida.UNIDAD);
        producto.setEstado(true);
        producto = productoRepository.save(producto);

        // Dar de baja 3 unidades por vencimiento
        int cantidadMerma = 3;
        LocalDate fechaCaducidad = LocalDate.now().minusDays(1);
        String observacion = "Lote vencido durante el fin de semana";

        Merma mermaGuardada = mermaService.registrarMerma(
                producto.getId(),
                usuario.getId(),
                cantidadMerma,
                MotivoMerma.VENCIMIENTO,
                fechaCaducidad,
                observacion
        );

        // 1. Validar registro de Merma
        assertNotNull(mermaGuardada.getId());
        assertEquals(cantidadMerma, mermaGuardada.getCantidad());
        assertEquals(MotivoMerma.VENCIMIENTO, mermaGuardada.getMotivo());
        assertEquals(0, new BigDecimal("5.50").compareTo(mermaGuardada.getCostoUnitario()));
        // Pérdida total: 3 * 5.50 = 16.50
        assertEquals(0, new BigDecimal("16.50").compareTo(mermaGuardada.getCostoTotalPerdida()));
        assertEquals(fechaCaducidad, mermaGuardada.getFechaVencimiento());

        // 2. Validar que el stock del producto disminuyó atómicamente
        Producto productoActualizado = productoRepository.findById(producto.getId()).orElseThrow();
        assertEquals(17, productoActualizado.getStockActual());

        // 3. Validar auditoría en Kardex (MovimientoInventario)
        List<MovimientoInventario> movimientos = movimientoRepository.findByProductoIdOrderByFechaMovimientoDesc(producto.getId());
        assertFalse(movimientos.isEmpty());
        MovimientoInventario ultimoMov = movimientos.get(0);
        assertEquals(TipoMovimiento.MERMA, ultimoMov.getTipoMovimiento());
        assertEquals(cantidadMerma, ultimoMov.getCantidad());
        assertEquals(20, ultimoMov.getStockAnterior());
        assertEquals(17, ultimoMov.getStockPosterior());
        assertTrue(ultimoMov.getMotivo().contains("MERMA"));
    }

    @Test
    @DisplayName("Debe lanzar excepción si se intenta dar de baja una cantidad superior al stock disponible")
    void testRegistrarMermaStockInsuficiente() {
        Empleado usuario = obtenerOCrearEmpleado();
        Categoria categoria = categoriaRepository.findAll().stream().findFirst().orElseThrow();

        Producto producto = new Producto();
        producto.setCodigo("PRD-TEST-FAIL-" + System.currentTimeMillis());
        producto.setNombre("Galletas Soda");
        producto.setMarca("San Jorge");
        producto.setCategoria(categoria);
        producto.setPrecioCompra(new BigDecimal("1.20"));
        producto.setPrecioVenta(new BigDecimal("2.00"));
        producto.setStockActual(5);
        producto.setStockMinimo(2);
        producto.setUnidadMedida(UnidadMedida.PAQUETE);
        producto.setEstado(true);
        producto = productoRepository.save(producto);

        // Intentar dar de baja 10 unidades teniendo solo 5
        Long prodId = producto.getId();
        Long usuId = usuario.getId();
        Exception ex = assertThrows(RuntimeException.class, () ->
                mermaService.registrarMerma(prodId, usuId, 10, MotivoMerma.ROTURA_DANO, null, "Paquete roto")
        );

        assertTrue(ex.getMessage().contains("Stock insuficiente"));
    }
}
