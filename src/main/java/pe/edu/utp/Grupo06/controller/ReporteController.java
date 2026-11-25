package pe.edu.utp.Grupo06.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.utp.Grupo06.dto.reporte.ResumenInventarioDTO;
import pe.edu.utp.Grupo06.dto.reporte.ResumenVentasDTO;
import pe.edu.utp.Grupo06.dto.venta.ProductoRotacionDTO;
import pe.edu.utp.Grupo06.service.IReporteService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@RestController
@RequestMapping("/api/reportes")
public class ReporteController {

    private static final ZoneId ZONA_LIMA = ZoneId.of("America/Lima");

    @Autowired
    private IReporteService reporteService;

    @GetMapping("/inventario")
    public ResponseEntity<ResumenInventarioDTO> obtenerResumenInventario() {
        return ResponseEntity.ok(reporteService.obtenerResumenInventario());
    }

    @GetMapping("/ventas")
    public ResponseEntity<ResumenVentasDTO> obtenerResumenVentas(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin) {
        if (inicio.isAfter(fin)) {
            throw new IllegalArgumentException("La fecha de inicio no puede ser posterior a la fecha de fin");
        }
        return ResponseEntity.ok(reporteService.obtenerResumenVentasPorPeriodo(inicio, fin));
    }

    @GetMapping("/ventas/rango")
    public ResponseEntity<ResumenVentasDTO> obtenerResumenVentasPorRango(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(reporteService.obtenerResumenVentasPorFechas(desde, hasta != null ? hasta : desde));
    }

    @GetMapping("/mayor-rotacion")
    public ResponseEntity<List<ProductoRotacionDTO>> obtenerProductosMayorRotacion(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin,
            @RequestParam(required = false) Long categoriaId) {
        if (inicio != null || fin != null || categoriaId != null) {
            LocalDate hoyLima = LocalDate.now(ZONA_LIMA);
            LocalDateTime inicioEfectivo = inicio != null ? inicio : LocalDate.of(2000, 1, 1).atStartOfDay();
            LocalDateTime finEfectivo = fin != null ? fin : hoyLima.plusDays(1).atStartOfDay();
            if (inicioEfectivo.isAfter(finEfectivo)) {
                throw new IllegalArgumentException("La fecha de inicio no puede ser posterior a la fecha de fin");
            }
            return ResponseEntity.ok(reporteService.obtenerProductosMayorRotacion(inicioEfectivo, finEfectivo, categoriaId));
        }
        return ResponseEntity.ok(reporteService.obtenerProductosMayorRotacion());
    }
}
