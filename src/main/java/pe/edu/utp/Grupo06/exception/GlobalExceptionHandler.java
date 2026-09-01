package pe.edu.utp.Grupo06.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ValidacionException.class)
    public ResponseEntity<ErrorResponse> manejarValidacionException(ValidacionException ex) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Error de Validación",
                "Uno o más campos no cumplen con las validaciones requeridas",
                ex.getErrores()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> manejarMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        List<String> errores = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.toList());

        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Datos de entrada inválidos",
                "Existen errores en la estructura de la solicitud",
                errores
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> manejarRuntimeException(RuntimeException ex) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        String mensaje = ex.getMessage() != null ? ex.getMessage() : "Ocurrió un error en la solicitud";

        if (mensaje.toLowerCase().contains("no encontrado") || mensaje.toLowerCase().contains("no encontrada")) {
            status = HttpStatus.NOT_FOUND;
        }

        ErrorResponse error = new ErrorResponse(
                status.value(),
                status.getReasonPhrase(),
                mensaje
        );
        return ResponseEntity.status(status).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> manejarExceptionGeneral(Exception ex) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Error Interno del Servidor",
                "Ocurrió un error inesperado. Por favor, intente nuevamente o contacte al administrador."
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
