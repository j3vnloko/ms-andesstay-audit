package cl.duoc.andesstay.audit.dto;

import cl.duoc.andesstay.audit.model.AuditEvent;

import java.time.Instant;

/**
 * DTO de salida: no exponemos la entidad JPA directamente ni el payload
 * crudo completo en el listado, para mantener la respuesta liviana.
 */
public class AuditEventResponse {

    private final Long id;
    private final String eventType;
    private final String aggregateId;
    private final String actor;
    private final Instant occurredAt;
    private final Instant receivedAt;
    private final String traceId;
    private final String correlationId;

    public AuditEventResponse(AuditEvent e) {
        this.id = e.getId();
        this.eventType = e.getEventType();
        this.aggregateId = e.getAggregateId();
        this.actor = e.getActor();
        this.occurredAt = e.getOccurredAt();
        this.receivedAt = e.getReceivedAt();
        this.traceId = e.getTraceId();
        this.correlationId = e.getCorrelationId();
    }

    public Long getId() {
        return id;
    }

    public String getEventType() {
        return eventType;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public String getActor() {
        return actor;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public String getTraceId() {
        return traceId;
    }

    public String getCorrelationId() {
        return correlationId;
    }
}
