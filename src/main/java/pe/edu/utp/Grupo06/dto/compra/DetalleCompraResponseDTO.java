package pe.edu.utp.Grupo06.dto.compra;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DetalleCompraResponseDTO {
    private Long id;
    private Long productoId;
    private String productoNombre;
    private String productoCodigo;
    private Integer cantidad;
    private BigDecimal precioUnitario;
    private BigDecimal subtotal;
}
