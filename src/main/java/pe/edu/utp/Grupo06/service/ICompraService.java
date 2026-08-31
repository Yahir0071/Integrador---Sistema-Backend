package pe.edu.utp.Grupo06.service;

import pe.edu.utp.Grupo06.model.Compra;
import java.time.LocalDateTime;
import java.util.List;

public interface ICompraService {
    Compra registrarCompra(Compra compra);
    List<Compra> listarCompras();
    List<Compra> listarPorFechas(LocalDateTime inicio, LocalDateTime fin);
    Compra buscarPorId(Long id);
}