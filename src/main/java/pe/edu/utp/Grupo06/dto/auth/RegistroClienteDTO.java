package pe.edu.utp.Grupo06.dto.auth;

import jakarta.validation.constraints.*;
import pe.edu.utp.Grupo06.util.MaxUtf8Bytes;

/** El cliente no puede elegir rol, ID ni estado de su cuenta. */
@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
public record RegistroClienteDTO(
        @NotBlank(message = "El nombre de usuario es obligatorio")
        @Pattern(regexp = "[A-Za-z0-9._-]{3,50}", message = "El usuario debe tener entre 3 y 50 caracteres válidos")
        String username,
        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
        @MaxUtf8Bytes(value = 72, message = "La contraseña no puede superar los 72 bytes UTF-8")
        String password,
        @NotBlank(message = "El nombre completo es obligatorio")
        @Size(max = 100, message = "El nombre completo no puede superar los 100 caracteres")
        @Pattern(regexp = "^[A-Za-zÁÉÍÓÚáéíóúÑñÜü ]+$",
                 message = "El nombre solo puede contener letras y espacios")
        String nombreCompleto,
        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato válido")
        @Size(max = 100, message = "El correo no puede superar los 100 caracteres")
        String email,
        @NotBlank(message = "El teléfono es obligatorio")
        @Size(max = 15, message = "El teléfono no puede superar los 15 caracteres")
        @Pattern(regexp = "^9\\d{8}$",
                 message = "Ingrese un número de celular peruano válido (9 dígitos comenzando con 9)")
        String telefono) {

    public RegistroClienteDTO {
        nombreCompleto = nombreCompleto == null ? null : nombreCompleto.trim().replaceAll("\\s+", " ");
        telefono = telefono == null ? null : telefono.trim();
    }

    /** Normaliza nombre y teléfono antes de consumirlos. */
    public String nombreCompletoNormalizado() {
        if (nombreCompleto == null) return null;
        return nombreCompleto.trim().replaceAll("\\s{2,}", " ");
    }

    public String telefonoNormalizado() {
        return telefono == null ? null : telefono.trim();
    }

    @Override
    public String toString() {
        return "RegistroClienteDTO[datos protegidos]";
    }
}
