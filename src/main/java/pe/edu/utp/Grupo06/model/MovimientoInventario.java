package pe.edu.utp.Grupo06.model;

import jakarta.persistence.*;
import lombok.*;
import pe.edu.utp.Grupo06.model.enums.TipoMovimiento;

import java.time.LocalDateTime;

@Entity
@Table(name = "movimientos_inventario")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class MovimientoInventario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_movimiento", nullable = false, length = 20)
    private TipoMovimiento tipoMovimiento;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(name = "stock_anterior", nullable = false)
    private Integer stockAnterior;

    @Column(name = "stock_posterior", nullable = false)
    private Integer stockPosterior;

    @Column(name = "fecha_movimiento", nullable = false)
    private LocalDateTime fechaMovimiento = LocalDateTime.now();

    @Column(length = 255)
    private String motivo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;
}
