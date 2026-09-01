package pe.edu.utp.Grupo06.dto.compra;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DetalleCompraRequestDTO {

    @NotNull(message = "El producto es obligatorio en el detalle")
    private Long productoId;

    @NotNull(message = "La cantidad es obligatoria")
    @Positive(message = "La cantidad debe ser mayor a 0")
    private Integer cantidad;

    @NotNull(message = "El precio unitario de compra es obligatorio")
    @PositiveOrZero(message = "El precio unitario no puede ser negativo")
    private BigDecimal precioUnitario;
}
