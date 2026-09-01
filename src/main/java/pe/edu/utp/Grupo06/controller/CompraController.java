package pe.edu.utp.Grupo06.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.utp.Grupo06.dto.compra.CompraRequestDTO;
import pe.edu.utp.Grupo06.dto.compra.CompraResponseDTO;
import pe.edu.utp.Grupo06.dto.compra.DetalleCompraRequestDTO;
import pe.edu.utp.Grupo06.dto.compra.DetalleCompraResponseDTO;
import pe.edu.utp.Grupo06.model.Compra;
import pe.edu.utp.Grupo06.model.DetalleCompra;
import pe.edu.utp.Grupo06.model.Producto;
import pe.edu.utp.Grupo06.model.Proveedor;
import pe.edu.utp.Grupo06.model.Usuario;
import pe.edu.utp.Grupo06.service.ICompraService;
import pe.edu.utp.Grupo06.service.IProductoService;
import pe.edu.utp.Grupo06.service.IProveedorService;
import pe.edu.utp.Grupo06.service.IUsuarioService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/compras")
@CrossOrigin(origins = "*")
public class CompraController {

    @Autowired
    private ICompraService compraService;

    @Autowired
    private IProveedorService proveedorService;

    @Autowired
    private IUsuarioService usuarioService;

    @Autowired
    private IProductoService productoService;

    @GetMapping
    public ResponseEntity<List<CompraResponseDTO>> listarCompras() {
        List<CompraResponseDTO> compras = compraService.listarCompras()
                .stream()
                .map(this::mapearADTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(compras);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CompraResponseDTO> buscarPorId(@PathVariable Long id) {
        Compra compra = compraService.buscarPorId(id);
        return ResponseEntity.ok(mapearADTO(compra));
    }

    @GetMapping("/rango")
    public ResponseEntity<List<CompraResponseDTO>> listarPorFechas(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin) {
        List<CompraResponseDTO> compras = compraService.listarPorFechas(inicio, fin)
                .stream()
                .map(this::mapearADTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(compras);
    }

    @PostMapping
    public ResponseEntity<CompraResponseDTO> registrarCompra(@Valid @RequestBody CompraRequestDTO request) {
        Proveedor proveedor = proveedorService.buscarPorId(request.getProveedorId());
        Usuario usuario = usuarioService.buscarPorId(request.getUsuarioId());

        Compra compra = new Compra();
        compra.setNumeroComprobante(request.getNumeroComprobante());
        compra.setProveedor(proveedor);
        compra.setUsuario(usuario);

        List<DetalleCompra> detalles = new ArrayList<>();
        for (DetalleCompraRequestDTO detDto : request.getDetalles()) {
            Producto producto = productoService.buscarPorId(detDto.getProductoId());
            DetalleCompra detalle = new DetalleCompra();
            detalle.setProducto(producto);
            detalle.setCantidad(detDto.getCantidad());
            detalle.setPrecioUnitario(detDto.getPrecioUnitario());
            detalles.add(detalle);
        }
        compra.setDetalles(detalles);

        Compra compraGuardada = compraService.registrarCompra(compra);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapearADTO(compraGuardada));
    }

    private CompraResponseDTO mapearADTO(Compra c) {
        List<DetalleCompraResponseDTO> detalles = c.getDetalles() != null ? c.getDetalles().stream().map(d ->
                new DetalleCompraResponseDTO(
                        d.getId(),
                        d.getProducto() != null ? d.getProducto().getId() : null,
                        d.getProducto() != null ? d.getProducto().getNombre() : null,
                        d.getProducto() != null ? d.getProducto().getCodigo() : null,
                        d.getCantidad(),
                        d.getPrecioUnitario(),
                        d.getSubtotal()
                )
        ).collect(Collectors.toList()) : List.of();

        return new CompraResponseDTO(
                c.getId(),
                c.getNumeroComprobante(),
                c.getFechaCompra(),
                c.getTotal(),
                c.getProveedor() != null ? c.getProveedor().getId() : null,
                c.getProveedor() != null ? c.getProveedor().getRazonSocial() : null,
                c.getUsuario() != null ? c.getUsuario().getId() : null,
                c.getUsuario() != null ? c.getUsuario().getNombreCompleto() : null,
                detalles
        );
    }
}
