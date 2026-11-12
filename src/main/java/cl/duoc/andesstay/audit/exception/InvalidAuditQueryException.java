package cl.duoc.andesstay.audit.exception;

/** Los filtros de /api/audit no son validos (se traduce a HTTP 400). */
public class InvalidAuditQueryException extends RuntimeException {

    public InvalidAuditQueryException(String message) {
        super(message);
    }
}
