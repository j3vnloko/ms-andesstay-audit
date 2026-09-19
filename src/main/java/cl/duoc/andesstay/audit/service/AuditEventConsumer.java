package cl.duoc.andesstay.audit.service;

import cl.duoc.andesstay.audit.dto.ReservationEventMessage;
import cl.duoc.andesstay.audit.model.AuditEvent;
import cl.duoc.andesstay.audit.repository.AuditEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class AuditEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(AuditEventConsumer.class);

    private final AuditEventRepository repository;
    private final ObjectMapper objectMapper;

    public AuditEventConsumer(AuditEventRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    /**
     * Escucha el topico "reservations.events" (definido en application.properties
     * para poder cambiarlo sin recompilar). Cada mensaje representa un evento de
     * negocio de una reserva (creada, confirmada, checkin, checkout, cancelada).
     *
     * Este consumer es deliberadamente tolerante a fallos: si un mensaje viene
     * mal formado o le faltan campos, se loguea el error y se sigue, en vez de
     * tumbar el consumer completo (eso dejaria de procesar TODOS los mensajes
     * siguientes, que es peor que perder uno malo).
     */
    @KafkaListener(
            topics = "${audit.kafka.topic}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void escuchar(String mensajeJson) {
        try {
            ReservationEventMessage evento = objectMapper.readValue(mensajeJson, ReservationEventMessage.class);
            guardar(evento, mensajeJson);
        } catch (Exception e) {
            log.error("No se pudo procesar un mensaje de reservations.events. Payload crudo: {}", mensajeJson, e);
        }
    }

    private void guardar(ReservationEventMessage evento, String payloadCrudo) {
        // eventId es la clave de idempotencia: si Kafka reentrega el mismo
        // mensaje (puede pasar, at-least-once delivery), no lo duplicamos.
        String eventId = evento.getEventId() != null ? evento.getEventId() : UUID.randomUUID().toString();

        if (repository.existsByEventId(eventId)) {
            log.info("Evento {} ya estaba registrado, se ignora (idempotencia)", eventId);
            return;
        }

        AuditEvent registro = new AuditEvent();
        registro.setEventId(eventId);
        registro.setEventType(evento.getType() != null ? evento.getType() : "DESCONOCIDO");
        registro.setAggregateId(evento.getReservationId());
        registro.setActor(evento.getActor());
        registro.setOccurredAt(evento.getTimestamp() != null ? evento.getTimestamp() : Instant.now());
        registro.setReceivedAt(Instant.now());
        registro.setTraceId(evento.getTraceId());
        registro.setCorrelationId(evento.getCorrelationId());
        registro.setPayload(payloadCrudo);

        repository.save(registro);
        log.info("Evento de auditoria guardado: tipo={}, reserva={}, actor={}",
                registro.getEventType(), registro.getAggregateId(), registro.getActor());
    }
}
