package pe.edu.utp.Grupo06.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String mensaje;
    private Object detalles;

    public ErrorResponse(int status, String error, String mensaje) {
        this.timestamp = LocalDateTime.now();
        this.status = status;
        this.error = error;
        this.mensaje = mensaje;
    }

    public ErrorResponse(int status, String error, String mensaje, Object detalles) {
        this(status, error, mensaje);
        this.detalles = detalles;
    }
}
