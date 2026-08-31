package pe.edu.utp.Grupo06.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "proveedores")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class Proveedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String ruc;

    @Column(name = "razon_social", nullable = false, length = 150)
    private String razonSocial;

    @Column(length = 20)
    private String telefono;

    @Column(length = 100)
    private String correo;

    @Column(length = 200)
    private String direccion;

    @Column(nullable = false)
    private Boolean estado = true;
}
