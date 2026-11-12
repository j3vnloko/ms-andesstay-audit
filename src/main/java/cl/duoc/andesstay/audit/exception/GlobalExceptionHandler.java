package cl.duoc.andesstay.audit.exception;

import cl.duoc.andesstay.audit.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Traduce los errores del endpoint de auditoria a respuestas JSON con el
 * mismo formato (ErrorResponse) que el resto de los microservicios, en vez
 * de dejar pasar stack traces o paginas HTML de error.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Filtros invalidos: rango de fechas al reves, size fuera de rango, etc. */
    @ExceptionHandler(InvalidAuditQueryException.class)
    public ResponseEntity<ErrorResponse> handleInvalidQuery(InvalidAuditQueryException ex,
                                                            HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    /** Un query param con formato invalido, ej: desde=ayer en vez de ISO-8601. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                            HttpServletRequest request) {
        String message = "El parametro '" + ex.getName() + "' tiene un formato invalido.";
        return build(HttpStatus.BAD_REQUEST, message, request);
    }

    /**
     * Red de seguridad: nunca se filtra un stack trace al cliente.
     *  - Los errores propios de Spring MVC (404 ruta inexistente, 405 metodo no
     *    permitido, falta un parametro...) implementan org.springframework.web.ErrorResponse
     *    y conservan su status, en vez de convertirse en 500.
     *  - Los errores de seguridad se relanzan para que los atienda Spring Security.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request)
            throws Exception {
        if (ex instanceof AccessDeniedException || ex instanceof AuthenticationException) {
            throw ex;
        }
        if (ex instanceof org.springframework.web.ErrorResponse springError) {
            HttpStatus status = HttpStatus.resolve(springError.getStatusCode().value());
            if (status != null && status.is4xxClientError()) {
                return build(status, status.getReasonPhrase(), request);
            }
        }
        log.error("Error no controlado en {}", request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocurrio un error interno. Intenta nuevamente.", request);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message,
                                                HttpServletRequest request) {
        // "bad_request", "not_found", "method_not_allowed"... mismo estilo que el resto de los servicios
        String error = status.name().toLowerCase();
        return ResponseEntity.status(status)
                .body(ErrorResponse.of(status.value(), error, message, request.getRequestURI()));
    }
}
