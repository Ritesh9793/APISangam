package com.apimarketplace.repository;

import com.apimarketplace.entity.AuditLog;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID>, JpaSpecificationExecutor<AuditLog> {
    List<AuditLog> findByActorIdOrderByCreatedAtDesc(UUID actorId);

    List<AuditLog> findAllByOrderByCreatedAtDesc();
}
