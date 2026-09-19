package cl.duoc.andesstay.audit.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;

/**
 * Representa el "envelope comun" que el caso AndesStay define para los
 * eventos: type, eventId, timestamp, traceId, correlationId - mas los
 * campos propios del dominio de reservas que necesitamos para auditoria
 * (aggregateId = id de la reserva, actor = quien hizo la accion).
 *
 * @JsonIgnoreProperties(ignoreUnknown = true) es importante: como todavia
 * no esta terminado ms-andesstay-reservations, este mapeo debe tolerar
 * campos adicionales que el equipo de reservations decida agregar despues,
 * sin que la deserializacion falle.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReservationEventMessage {

    private String eventId;
    private String type;
    private Instant timestamp;
    private String traceId;
    private String correlationId;

    // Campos esperables del dominio de reservas; pueden venir null si el
    // productor todavia no los define exactamente asi - por eso el consumer
    // no falla si faltan, solo guarda lo que llegue.
    private String reservationId;
    private String actor;

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public String getReservationId() {
        return reservationId;
    }

    public void setReservationId(String reservationId) {
        this.reservationId = reservationId;
    }

    public String getActor() {
        return actor;
    }

    public void setActor(String actor) {
        this.actor = actor;
    }
}
