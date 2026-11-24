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
import java.util.List;

@RestController
@RequestMapping("/api/reportes")
public class ReporteController {

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
        return ResponseEntity.ok(reporteService.obtenerResumenVentasPorPeriodo(inicio, fin));
    }

    @GetMapping("/ventas/rango")
    public ResponseEntity<ResumenVentasDTO> obtenerResumenVentasPorRango(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(reporteService.obtenerResumenVentasPorFechas(desde, hasta != null ? hasta : desde));
    }

    @GetMapping("/mayor-rotacion")
    public ResponseEntity<List<ProductoRotacionDTO>> obtenerProductosMayorRotacion() {
        return ResponseEntity.ok(reporteService.obtenerProductosMayorRotacion());
    }
}
