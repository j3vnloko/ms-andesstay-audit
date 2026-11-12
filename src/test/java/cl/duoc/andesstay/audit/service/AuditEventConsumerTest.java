package cl.duoc.andesstay.audit.service;

import cl.duoc.andesstay.audit.exception.InvalidEventException;
import cl.duoc.andesstay.audit.model.AuditEvent;
import cl.duoc.andesstay.audit.repository.AuditEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuditEventConsumerTest {

    private static final String EVENTO_VALIDO = """
            {"eventId":"e-1","type":"RESERVA_CONFIRMADA","timestamp":"2026-10-01T10:00:00Z",
             "traceId":"t-1","correlationId":null,"reservationId":"42","actor":"recepcion@andesstay.cl",
             "unitId":7}
            """;

    private AuditEventRepository repository;
    private AuditEventConsumer consumer;

    @BeforeEach
    void setUp() {
        repository = mock(AuditEventRepository.class);
        consumer = new AuditEventConsumer(repository, new ObjectMapper().registerModule(new JavaTimeModule()));
    }

    @Test
    void guardaUnEventoValidoConSusCampos() {
        consumer.escuchar(EVENTO_VALIDO);

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(repository).save(captor.capture());
        AuditEvent guardado = captor.getValue();
        assertEquals("e-1", guardado.getEventId());
        assertEquals("RESERVA_CONFIRMADA", guardado.getEventType());
        assertEquals("42", guardado.getAggregateId());
        assertEquals("recepcion@andesstay.cl", guardado.getActor());
    }

    @Test
    void ignoraCamposDesconocidosDelProductor() {
        // "unitId" no esta en el DTO de audit: no debe romper la deserializacion.
        consumer.escuchar(EVENTO_VALIDO);
        verify(repository).save(any(AuditEvent.class));
    }

    @Test
    void ignoraUnEventoRepetidoPorIdempotencia() {
        when(repository.existsByEventId("e-1")).thenReturn(true);

        consumer.escuchar(EVENTO_VALIDO);

        verify(repository, never()).save(any(AuditEvent.class));
    }

    @Test
    void ignoraLaCarreraEntreDosEntregasSimultaneas() {
        when(repository.save(any(AuditEvent.class))).thenThrow(new DataIntegrityViolationException("uk_audit_event_id"));

        consumer.escuchar(EVENTO_VALIDO); // no debe lanzar
    }

    @Test
    void jsonRotoLanzaInvalidEventException() {
        assertThrows(InvalidEventException.class, () -> consumer.escuchar("esto no es json"));
        verify(repository, never()).save(any(AuditEvent.class));
    }

    @Test
    void eventoSinEventIdLanzaInvalidEventException() {
        assertThrows(InvalidEventException.class,
                () -> consumer.escuchar("{\"type\":\"RESERVA_CREADA\",\"reservationId\":\"1\"}"));
    }

    @Test
    void eventoSinTypeLanzaInvalidEventException() {
        assertThrows(InvalidEventException.class,
                () -> consumer.escuchar("{\"eventId\":\"e-2\",\"reservationId\":\"1\"}"));
    }

    @Test
    void fallaDeBaseDeDatosSePropagaParaQueSeReintente() {
        when(repository.save(any(AuditEvent.class))).thenThrow(new IllegalStateException("db caida"));

        assertThrows(IllegalStateException.class, () -> consumer.escuchar(EVENTO_VALIDO));
    }
}
