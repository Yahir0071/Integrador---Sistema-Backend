package pe.edu.utp.Grupo06.service;

import pe.edu.utp.Grupo06.model.Venta;
import java.time.LocalDateTime;
import java.util.List;

public interface IVentaService {
    Venta registrarVenta(Venta venta);
    List<Venta> listarVentas();
    List<Venta> listarPorFechas(LocalDateTime inicio, LocalDateTime fin);
    Venta buscarPorId(Long id);
    Venta buscarPorTicket(String ticket);
    List<Object[]> reporteMayorRotacion();
}