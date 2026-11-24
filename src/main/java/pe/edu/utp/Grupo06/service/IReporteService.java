package pe.edu.utp.Grupo06.service;

import pe.edu.utp.Grupo06.dto.reporte.ResumenInventarioDTO;
import pe.edu.utp.Grupo06.dto.reporte.ResumenVentasDTO;
import pe.edu.utp.Grupo06.dto.venta.ProductoRotacionDTO;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface IReporteService {
    ResumenInventarioDTO obtenerResumenInventario();
    ResumenVentasDTO obtenerResumenVentasPorPeriodo(LocalDateTime inicio, LocalDateTime fin);
    ResumenVentasDTO obtenerResumenVentasPorFechas(LocalDate desde, LocalDate hasta);
    List<ProductoRotacionDTO> obtenerProductosMayorRotacion();
}
