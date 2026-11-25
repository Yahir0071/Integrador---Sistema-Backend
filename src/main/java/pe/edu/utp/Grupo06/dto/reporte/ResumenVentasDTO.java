package pe.edu.utp.Grupo06.dto.reporte;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResumenVentasDTO {
    private Long cantidadVentasEmitidas;
    private Long cantidadVentasAnuladas;
    private BigDecimal totalRecaudado;
    private Long unidadesVendidas;
    private BigDecimal ticketPromedio;

    /** Constructor de compatibilidad con 3 argumentos. */
    public ResumenVentasDTO(Long cantidadVentasEmitidas, Long cantidadVentasAnuladas, BigDecimal totalRecaudado) {
        this(cantidadVentasEmitidas, cantidadVentasAnuladas, totalRecaudado, 0L, BigDecimal.ZERO);
    }

    /** Constructor de compatibilidad con 4 argumentos. */
    public ResumenVentasDTO(Long cantidadVentasEmitidas, Long cantidadVentasAnuladas, BigDecimal totalRecaudado, Long unidadesVendidas) {
        this(cantidadVentasEmitidas, cantidadVentasAnuladas, totalRecaudado, unidadesVendidas, BigDecimal.ZERO);
    }
}
