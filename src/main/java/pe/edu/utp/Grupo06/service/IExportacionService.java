package pe.edu.utp.Grupo06.service;

import pe.edu.utp.Grupo06.model.Compra;
import pe.edu.utp.Grupo06.model.Merma;
import pe.edu.utp.Grupo06.model.Producto;
import pe.edu.utp.Grupo06.model.Venta;

import java.io.File;
import java.time.LocalDate;
import java.util.List;

public interface IExportacionService {

    void exportarVentasCsv(File destino, List<Venta> ventas, LocalDate inicio, LocalDate fin) throws Exception;

    void exportarComprasCsv(File destino, List<Compra> compras, LocalDate inicio, LocalDate fin) throws Exception;

    void exportarMermasCsv(File destino, List<Merma> mermas, LocalDate inicio, LocalDate fin) throws Exception;

    void exportarInventarioCsv(File destino, List<Producto> productos) throws Exception;
}
