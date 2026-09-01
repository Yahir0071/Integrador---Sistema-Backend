package pe.edu.utp.Grupo06.model;

import jakarta.persistence.*;
import lombok.*;
import pe.edu.utp.Grupo06.model.enums.MetodoPago;

import java.math.BigDecimal;

@Entity
@Table(name = "pagos")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venta_id", nullable = false)
    private Venta venta;

    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_pago", nullable = false, length = 30)
    private MetodoPago metodoPago;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;
}