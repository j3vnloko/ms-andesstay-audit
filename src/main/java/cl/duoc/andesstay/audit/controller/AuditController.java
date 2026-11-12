package cl.duoc.andesstay.audit.controller;

import cl.duoc.andesstay.audit.dto.AuditEventResponse;
import cl.duoc.andesstay.audit.exception.InvalidAuditQueryException;
import cl.duoc.andesstay.audit.repository.AuditEventRepository;
import cl.duoc.andesstay.audit.repository.AuditEventSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/**
 * Endpoint de solo lectura, como lo define el caso: "/audit - Admin, Auditor -
 * Trazabilidad de la reserva. Filtros: usuario, fechas, tipo de evento."
 *
 * La restriccion de que solo ADMIN y AUDITOR puedan llamarlo se aplica en
 * SecurityConfig, no aqui - este controller no valida roles el mismo.
 */
@RestController
public class AuditController {

    static final int MAX_PAGE_SIZE = 100;

    private final AuditEventRepository repository;

    public AuditController(AuditEventRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/api/audit")
    public Page<AuditEventResponse> buscar(
            @RequestParam(required = false) String usuario,
            @RequestParam(required = false) String tipoEvento,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant hasta,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        if (page < 0) {
            throw new InvalidAuditQueryException("page no puede ser negativo.");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new InvalidAuditQueryException("size debe estar entre 1 y " + MAX_PAGE_SIZE + ".");
        }
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new InvalidAuditQueryException("'desde' no puede ser posterior a 'hasta'.");
        }

        var filtro = AuditEventSpecifications.conFiltros(usuario, tipoEvento, desde, hasta);
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "occurredAt"));

        return repository
                .findAll(filtro, pageable)
                .map(AuditEventResponse::new);
    }
}
