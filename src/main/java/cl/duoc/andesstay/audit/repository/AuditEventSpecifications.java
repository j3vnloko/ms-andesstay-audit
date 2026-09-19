package cl.duoc.andesstay.audit.repository;

import cl.duoc.andesstay.audit.model.AuditEvent;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;

/**
 * Construye los filtros dinamicos de /api/audit usando la Criteria API en
 * vez de JPQL con "?/:param IS NULL OR columna = ?". Esa forma con JPQL
 * funciona con H2 pero falla con PostgreSQL ("no se pudo determinar el tipo
 * del parametro") porque Postgres necesita saber el tipo de cada parametro
 * de antemano y no lo puede inferir en una clausula "OR ... IS NULL" sola.
 *
 * Con Specification, cada filtro solo se agrega al WHERE si el valor no es
 * null - no hay parametros "IS NULL" flotando sin tipo, asi que Postgres
 * nunca tiene ese problema.
 */
public class AuditEventSpecifications {

    private AuditEventSpecifications() {
    }

    public static Specification<AuditEvent> conFiltros(String actor, String eventType, Instant desde, Instant hasta) {
        return (root, query, cb) -> {
            var predicates = cb.conjunction();

            if (actor != null && !actor.isBlank()) {
                predicates = cb.and(predicates, cb.equal(root.get("actor"), actor));
            }
            if (eventType != null && !eventType.isBlank()) {
                predicates = cb.and(predicates, cb.equal(root.get("eventType"), eventType));
            }
            if (desde != null) {
                predicates = cb.and(predicates, cb.greaterThanOrEqualTo(root.get("occurredAt"), desde));
            }
            if (hasta != null) {
                predicates = cb.and(predicates, cb.lessThanOrEqualTo(root.get("occurredAt"), hasta));
            }

            return predicates;
        };
    }
}
