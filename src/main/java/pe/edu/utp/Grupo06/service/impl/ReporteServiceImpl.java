package pe.edu.utp.Grupo06.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.utp.Grupo06.dto.reporte.ResumenInventarioDTO;
import pe.edu.utp.Grupo06.dto.reporte.ResumenVentasDTO;
import pe.edu.utp.Grupo06.dto.venta.ProductoRotacionDTO;
import pe.edu.utp.Grupo06.model.Producto;
import pe.edu.utp.Grupo06.model.Venta;
import pe.edu.utp.Grupo06.model.enums.EstadoAlerta;
import pe.edu.utp.Grupo06.model.enums.EstadoVenta;
import pe.edu.utp.Grupo06.repository.AlertaReposicionRepository;
import pe.edu.utp.Grupo06.repository.DetalleVentaRepository;
import pe.edu.utp.Grupo06.repository.ProductoRepository;
import pe.edu.utp.Grupo06.repository.VentaRepository;
import pe.edu.utp.Grupo06.service.IReporteService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReporteServiceImpl implements IReporteService {

    public static final ZoneId ZONA_LIMA = ZoneId.of("America/Lima");

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private AlertaReposicionRepository alertaRepository;

    @Autowired
    private VentaRepository ventaRepository;

    @Autowired
    private DetalleVentaRepository detalleVentaRepository;

    @Override
    @Transactional(readOnly = true)
    public ResumenInventarioDTO obtenerResumenInventario() {
        List<Producto> productosActivos = productoRepository.findByEstadoTrue();

        long totalActivos = productosActivos.size();
        long bajoStock = productosActivos.stream()
                .filter(p -> p.getStockActual() <= p.getStockMinimo())
                .count();

        long alertasPendientes = alertaRepository.findByEstadoOrderByFechaGeneracionDesc(EstadoAlerta.PENDIENTE).size();

        BigDecimal valorizacion = productosActivos.stream()
                .map(p -> p.getPrecioCompra().multiply(BigDecimal.valueOf(p.getStockActual())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new ResumenInventarioDTO(totalActivos, bajoStock, alertasPendientes, valorizacion);
    }

    @Override
    @Transactional(readOnly = true)
    public ResumenVentasDTO obtenerResumenVentasPorPeriodo(LocalDateTime inicio, LocalDateTime fin) {
        if (inicio == null || fin == null) {
            throw new IllegalArgumentException("Las fechas de inicio y fin son obligatorias");
        }
        if (inicio.isAfter(fin)) {
            throw new IllegalArgumentException("La fecha de inicio no puede ser posterior a la fecha de fin");
        }
        List<Venta> ventas = ventaRepository.findByRangoSemiabiertoOrderByFechaVentaDesc(inicio, fin);
        return calcularResumen(ventas, inicio, fin);
    }

    @Override
    @Transactional(readOnly = true)
    public ResumenVentasDTO obtenerResumenVentasPorFechas(LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null || desde.isAfter(hasta)) {
            throw new IllegalArgumentException("Rango de fechas inválido: la fecha inicial no puede ser posterior a la fecha final");
        }
        LocalDateTime inicio = desde.atStartOfDay();
        LocalDateTime finExclusivo = hasta.plusDays(1).atStartOfDay();
        List<Venta> ventas = ventaRepository.findByRangoSemiabiertoOrderByFechaVentaDesc(inicio, finExclusivo);
        return calcularResumen(ventas, inicio, finExclusivo);
    }

    private ResumenVentasDTO calcularResumen(List<Venta> ventas, LocalDateTime inicio, LocalDateTime finExclusivo) {
        long emitidas = ventas.stream().filter(v -> !v.isDemo()).filter(v -> v.getEstado() == EstadoVenta.EMITIDA).count();
        long anuladas = ventas.stream().filter(v -> !v.isDemo()).filter(v -> v.getEstado() == EstadoVenta.ANULADA).count();

        BigDecimal totalRecaudado = ventas.stream()
                .filter(v -> !v.isDemo()).filter(v -> v.getEstado() == EstadoVenta.EMITIDA)
                .map(Venta::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Long unidades = detalleVentaRepository.sumUnidadesVendidasEntreFechas(inicio, finExclusivo);
        BigDecimal ticketPromedio = emitidas > 0
                ? totalRecaudado.divide(BigDecimal.valueOf(emitidas), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return new ResumenVentasDTO(emitidas, anuladas, totalRecaudado, unidades != null ? unidades : 0L, ticketPromedio);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductoRotacionDTO> obtenerProductosMayorRotacion() {
        return mapearRotacion(detalleVentaRepository.findProductosMayorRotacion());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductoRotacionDTO> obtenerProductosMayorRotacion(LocalDateTime inicio, LocalDateTime fin, Long categoriaId) {
        if (inicio == null && fin == null && categoriaId == null) {
            return obtenerProductosMayorRotacion();
        }
        LocalDate hoyLima = LocalDate.now(ZONA_LIMA);
        LocalDateTime inicioEfectivo = inicio != null ? inicio : LocalDate.of(2000, 1, 1).atStartOfDay();
        LocalDateTime finEfectivo = fin != null ? fin : hoyLima.plusDays(1).atStartOfDay();
        if (inicioEfectivo.isAfter(finEfectivo)) {
            throw new IllegalArgumentException("La fecha de inicio no puede ser posterior a la fecha de fin");
        }
        return mapearRotacion(detalleVentaRepository.findProductosMayorRotacionFiltrado(inicioEfectivo, finEfectivo, categoriaId));
    }

    private List<ProductoRotacionDTO> mapearRotacion(List<Object[]> resultados) {
        return resultados.stream().map(fila -> {
            Number idNum = (Number) fila[0];
            Long productoId = idNum != null ? idNum.longValue() : null;
            String codigo = (String) fila[1];
            String nombre = (String) fila[2];
            Number cantNum = (Number) fila[3];
            Long totalCantidad = cantNum != null ? cantNum.longValue() : 0L;
            BigDecimal totalRecaudado = fila[4] instanceof BigDecimal ? (BigDecimal) fila[4] : BigDecimal.ZERO;
            return new ProductoRotacionDTO(
                    productoId,
                    nombre,
                    codigo,
                    totalCantidad,
                    totalRecaudado
            );
        }).collect(Collectors.toList());
    }
}
