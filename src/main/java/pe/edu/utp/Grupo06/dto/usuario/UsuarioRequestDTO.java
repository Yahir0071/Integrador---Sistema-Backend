package pe.edu.utp.Grupo06.dto.usuario;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.edu.utp.Grupo06.model.enums.RolNombre;
import pe.edu.utp.Grupo06.util.MaxUtf8Bytes;

@Getter
@Setter
@NoArgsConstructor
public class UsuarioRequestDTO {

    @NotBlank(message = "El nombre de usuario es obligatorio")
    @Size(min = 3, max = 50, message = "El username debe tener entre 3 y 50 caracteres")
    private String username;

    @Size(max = 72, message = "La contraseña no puede superar los 72 caracteres")
    @MaxUtf8Bytes(value = 72, message = "La contraseña no puede superar los 72 bytes UTF-8")
    private String password;

    @NotBlank(message = "El nombre completo es obligatorio")
    @Size(max = 100, message = "El nombre completo no puede superar los 100 caracteres")
    @Pattern(regexp = "^[A-Za-zÁÉÍÓÚáéíóúÑñÜü ]+$",
             message = "El nombre solo puede contener letras y espacios")
    private String nombreCompleto;

    @Email(message = "El formato del correo electrónico no es válido")
    private String email;

    @Size(max = 15, message = "El teléfono no puede superar los 15 caracteres")
    @Pattern(regexp = "^$|^9\\d{8}$",
             message = "Ingrese un número de celular peruano válido (9 dígitos comenzando con 9)")
    private String telefono;

    @NotNull(message = "El rol es obligatorio")
    private RolNombre rol;

    public UsuarioRequestDTO(String username, String password, String nombreCompleto,
                             String email, String telefono, RolNombre rol) {
        this.username = username;
        this.password = password;
        setNombreCompleto(nombreCompleto);
        this.email = email;
        setTelefono(telefono);
        this.rol = rol;
    }

    public void setNombreCompleto(String nombreCompleto) {
        this.nombreCompleto = normalizarNombre(nombreCompleto);
    }

    public void setTelefono(String telefono) {
        this.telefono = normalizarTelefono(telefono);
    }

    /** Normaliza nombre: trim + colapsar espacios internos. */
    public String getNombreCompletoNormalizado() {
        return normalizarNombre(nombreCompleto);
    }

    public String getTelefonoNormalizado() {
        return normalizarTelefono(telefono);
    }

    private static String normalizarNombre(String valor) {
        if (valor == null) return null;
        return valor.trim().replaceAll("\\s+", " ");
    }

    private static String normalizarTelefono(String valor) {
        return valor == null ? null : valor.trim();
    }
}
