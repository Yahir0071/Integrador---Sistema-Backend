package pe.edu.utp.Grupo06.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String MENSAJE_ERROR_INTERNO = "Ocurrió un error inesperado al procesar la solicitud";

    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> archivoGrande(Exception ex) {
        return ResponseEntity.status(413)
                .body(new ErrorResponse(413, "Payload Too Large",
                        "Adjunte una imagen PNG o JPEG de hasta 3 MB"));
    }

    @ExceptionHandler(PedidoConflictException.class)
    public ResponseEntity<ErrorResponse> pedidoConflict(PedidoConflictException ex) {
        return ResponseEntity.status(409)
                .body(new ErrorResponse(409, "Conflict", ex.getMessage()));
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> manejarConflicto(Exception ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(409, "Conflict",
                        "Los datos ya existen o no cumplen las restricciones requeridas"));
    }

    @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
    public ResponseEntity<ErrorResponse> manejarAutenticacion(Exception ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse(401, "Unauthorized",
                        "Usuario o contraseña incorrectos"));
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> manejarAccesoDenegado(Exception ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse(403, "Forbidden",
                        "No tiene permisos para esta operación"));
    }

    @ExceptionHandler(ValidacionException.class)
    public ResponseEntity<ErrorResponse> manejarValidacionException(ValidacionException ex) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Error de Validación",
                "Uno o más campos no cumplen con las validaciones requeridas",
                ex.getErrores());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> manejarMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        Map<String, String> camposMensaje = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(err -> camposMensaje.putIfAbsent(err.getField(), err.getDefaultMessage()));

        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Datos de entrada inválidos",
                "Existen errores en la estructura de la solicitud",
                camposMensaje);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class
    })
    public ResponseEntity<ErrorResponse> manejarSolicitudMalFormada(Exception ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(),
                        "Bad Request",
                        "El formato de los datos o parámetros enviados no es válido"));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> manejarRecursoNoEncontrado(NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(HttpStatus.NOT_FOUND.value(),
                        "Not Found",
                        "El recurso solicitado no fue encontrado"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> manejarIllegalArgument(IllegalArgumentException ex) {
        String mensaje = ex.getMessage() != null ? ex.getMessage() : "Datos inválidos";
        HttpStatus status = HttpStatus.BAD_REQUEST;
        if (mensaje.toLowerCase().contains("no encontrad")) {
            status = HttpStatus.NOT_FOUND;
        }
        return ResponseEntity.status(status)
                .body(new ErrorResponse(status.value(), status.getReasonPhrase(), mensaje));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> manejarIllegalState(IllegalStateException ex) {
        String mensaje = ex.getMessage() != null ? ex.getMessage() : "No se puede completar la operación en el estado actual";
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(HttpStatus.CONFLICT.value(), "Conflict", mensaje));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> manejarRuntimeException(RuntimeException ex) {
        log.error("Excepción no controlada", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        "Internal Server Error",
                        MENSAJE_ERROR_INTERNO));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> manejarExceptionGeneral(Exception ex) {
        log.error("Error interno", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        "Internal Server Error",
                        MENSAJE_ERROR_INTERNO));
    }
}
