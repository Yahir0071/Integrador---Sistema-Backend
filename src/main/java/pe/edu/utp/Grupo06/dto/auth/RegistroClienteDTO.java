package pe.edu.utp.Grupo06.dto.auth;

import jakarta.validation.constraints.*;

/** El cliente no puede elegir rol, ID ni estado de su cuenta. */
@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
public record RegistroClienteDTO(
        @NotBlank(message = "El nombre de usuario es obligatorio")
        @Pattern(regexp = "[A-Za-z0-9._-]{3,50}", message = "El usuario debe tener entre 3 y 50 caracteres válidos")
        String username,
        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
        String password,
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
    @Override
    public String toString() {
        return "RegistroClienteDTO[datos protegidos]";
    }
}
