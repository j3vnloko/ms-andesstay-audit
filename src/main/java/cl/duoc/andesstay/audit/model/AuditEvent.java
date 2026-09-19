package cl.duoc.andesstay.audit.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

/**
 * Registro de un evento de negocio ocurrido en la reserva (creacion,
 * confirmacion, check-in, check-out, cancelacion, etc).
 *
 * Este microservicio NO genera estos eventos, solo los CONSUME desde el
 * topico Kafka "reservations.events" que publica ms-andesstay-reservations,
 * y los deja guardados para trazabilidad ("quien / que / cuando / desde donde").
 */
@Entity
@Table(name = "audit_events", uniqueConstraints = {
        @UniqueConstraint(name = "uk_audit_event_id", columnNames = "event_id")
})
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Id unico del evento tal como lo genero el productor (reservations).
    // Sirve para no duplicar si Kafka reentrega el mismo mensaje (idempotencia).
    @Column(name = "event_id", nullable = false)
    private String eventId;

    // Tipo de evento, ej: RESERVA_CREADA, RESERVA_CONFIRMADA, CHECKIN, CHECKOUT, CANCELADA
    @Column(name = "event_type", nullable = false)
    private String eventType;

    // Id de la reserva a la que pertenece el evento
    @Column(name = "aggregate_id")
    private String aggregateId;

    // Quien disparo la accion (username/email/sub del JWT que uso reservations)
    @Column(name = "actor")
    private String actor;

    // Cuando ocurrio el evento segun quien lo publico
    @Column(name = "occurred_at")
    private Instant occurredAt;

    // Cuando lo recibimos y guardamos nosotros
    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(name = "trace_id")
    private String traceId;

    @Column(name = "correlation_id")
    private String correlationId;

    // JSON completo tal como llego, por si se necesita revisar algo que no mapeamos explicitamente
    @Column(name = "payload", columnDefinition = "TEXT")
    private String payload;

    public AuditEvent() {
    }

    public Long getId() {
        return id;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public void setAggregateId(String aggregateId) {
        this.aggregateId = aggregateId;
    }

    public String getActor() {
        return actor;
    }

    public void setActor(String actor) {
        this.actor = actor;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(Instant receivedAt) {
        this.receivedAt = receivedAt;
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

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }
}
