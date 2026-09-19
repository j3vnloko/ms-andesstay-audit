package cl.duoc.andesstay.audit.repository;

import cl.duoc.andesstay.audit.model.AuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AuditEventRepository extends JpaRepository<AuditEvent, Long>,
        JpaSpecificationExecutor<AuditEvent> {

    boolean existsByEventId(String eventId);
}
