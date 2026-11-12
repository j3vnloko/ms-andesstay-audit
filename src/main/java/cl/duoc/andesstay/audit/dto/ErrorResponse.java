package cl.duoc.andesstay.audit.dto;

import java.time.Instant;

/**
 * Formato unico de error, igual que en ms-andesstay-bff y ms-andesstay-reservations:
 * { timestamp, status, error, message, path }.
 */
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path
) {
    public static ErrorResponse of(int status, String error, String message, String path) {
        return new ErrorResponse(Instant.now(), status, error, message, path);
    }
}
