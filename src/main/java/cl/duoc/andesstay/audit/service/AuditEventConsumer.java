package cl.duoc.andesstay.audit.service;

import cl.duoc.andesstay.audit.dto.ReservationEventMessage;
import cl.duoc.andesstay.audit.exception.InvalidEventException;
import cl.duoc.andesstay.audit.model.AuditEvent;
import cl.duoc.andesstay.audit.repository.AuditEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Consumer del topico "reservations.events" (definido en application.properties).
 * Cada mensaje es un evento de negocio de una reserva (creada, confirmada,
 * checkin, checkout, cancelada) y se guarda para trazabilidad.
 *
 * Manejo de errores (ver KafkaConsumerConfig):
 *  - Mensaje invalido (JSON roto, sin eventId o sin type): InvalidEventException,
 *    no se reintenta y va directo al topico DLT.
 *  - Falla transitoria (ej: Postgres caido): la excepcion se propaga, se
 *    reintenta 3 veces y, si sigue fallando, el mensaje va al DLT.
 * Asi no se pierde ningun mensaje ni se bloquea el consumer.
 */
@Service
public class AuditEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(AuditEventConsumer.class);

    private final AuditEventRepository repository;
    private final ObjectMapper objectMapper;

    public AuditEventConsumer(AuditEventRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = "${audit.kafka.topic}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void escuchar(String mensajeJson) {
        ReservationEventMessage evento = parsear(mensajeJson);
        guardar(evento, mensajeJson);
    }

    private ReservationEventMessage parsear(String mensajeJson) {
        ReservationEventMessage evento;
        try {
            evento = objectMapper.readValue(mensajeJson, ReservationEventMessage.class);
        } catch (JsonProcessingException | IllegalArgumentException e) {
            throw new InvalidEventException("El mensaje no es un JSON de evento valido.", e);
        }
        if (evento == null || isBlank(evento.getEventId()) || isBlank(evento.getType())) {
            throw new InvalidEventException("El evento no trae eventId o type (envelope incompleto).");
        }
        return evento;
    }

    void guardar(ReservationEventMessage evento, String payloadCrudo) {
        // eventId es la clave de idempotencia: si Kafka reentrega el mismo
        // mensaje (at-least-once), no lo duplicamos.
        String eventId = evento.getEventId();

        if (repository.existsByEventId(eventId)) {
            log.info("Evento {} ya estaba registrado, se ignora (idempotencia)", eventId);
            return;
        }

        AuditEvent registro = new AuditEvent();
        registro.setEventId(eventId);
        registro.setEventType(evento.getType());
        registro.setAggregateId(evento.getReservationId());
        registro.setActor(evento.getActor());
        registro.setOccurredAt(evento.getTimestamp() != null ? evento.getTimestamp() : Instant.now());
        registro.setReceivedAt(Instant.now());
        registro.setTraceId(evento.getTraceId());
        registro.setCorrelationId(evento.getCorrelationId());
        registro.setPayload(payloadCrudo);

        try {
            repository.save(registro);
        } catch (DataIntegrityViolationException e) {
            // Dos entregas simultaneas del mismo evento: gano la otra (uk_audit_event_id).
            log.info("Evento {} ya fue guardado por otra entrega, se ignora (idempotencia)", eventId);
            return;
        }
        log.info("Evento de auditoria guardado: tipo={}, reserva={}, actor={}",
                registro.getEventType(), registro.getAggregateId(), registro.getActor());
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
