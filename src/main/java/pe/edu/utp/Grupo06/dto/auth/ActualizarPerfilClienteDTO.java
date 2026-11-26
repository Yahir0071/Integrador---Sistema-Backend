package pe.edu.utp.Grupo06.dto.auth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.*;

/** Datos editables por el cliente autenticado (no permite alterar id, username, rol ni estado). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ActualizarPerfilClienteDTO(
        @NotBlank(message = "El nombre completo es obligatorio")
        @Size(max = 100, message = "El nombre completo no puede superar los 100 caracteres")
        String nombreCompleto,
        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato válido")
        @Size(max = 100, message = "El correo no puede superar los 100 caracteres")
        String email,
        @NotBlank(message = "El teléfono es obligatorio")
        @Pattern(regexp = "(?=(?:.*\\d){7,15})[0-9+ ()-]{7,15}", message = "El teléfono debe contener entre 7 y 15 dígitos válidos")
        String telefono) {
}
