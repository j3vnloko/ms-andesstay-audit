package cl.duoc.andesstay.audit.exception;

/**
 * El mensaje de Kafka no es un evento valido (JSON roto o sin eventId/type).
 * Reintentarlo no sirve de nada, asi que el KafkaConsumerConfig lo manda
 * directo al topico DLT sin reintentos.
 */
public class InvalidEventException extends RuntimeException {

    public InvalidEventException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidEventException(String message) {
        super(message);
    }
}
