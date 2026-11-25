package pe.edu.utp.Grupo06.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.edu.utp.Grupo06.util.MaxUtf8Bytes;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequestDTO {

    @NotBlank(message = "El nombre de usuario es obligatorio")
    @Size(max = 50, message = "El nombre de usuario no puede superar los 50 caracteres")
    private String username;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(max = 72, message = "La contraseña no puede superar los 72 caracteres")
    @MaxUtf8Bytes(value = 72, message = "La contraseña no puede superar los 72 bytes UTF-8")
    private String password;
}
