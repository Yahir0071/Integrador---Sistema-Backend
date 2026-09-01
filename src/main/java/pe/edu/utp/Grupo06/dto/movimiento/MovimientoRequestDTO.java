package pe.edu.utp.Grupo06.dto.movimiento;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.edu.utp.Grupo06.model.enums.TipoMovimiento;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MovimientoRequestDTO {

    @NotNull(message = "El producto es obligatorio")
    private Long productoId;

    @NotNull(message = "El usuario es obligatorio")
    private Long usuarioId;

    @NotNull(message = "El tipo de movimiento es obligatorio")
    private TipoMovimiento tipoMovimiento;

    @NotNull(message = "La cantidad es obligatoria")
    @Positive(message = "La cantidad debe ser mayor a 0")
    private Integer cantidad;

    @Size(max = 255, message = "El motivo no puede superar los 255 caracteres")
    private String motivo;
}
