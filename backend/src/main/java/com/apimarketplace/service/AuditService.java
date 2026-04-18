package com.apimarketplace.service;

import com.apimarketplace.dto.audit.AuditLogResponse;
import com.apimarketplace.entity.AuditLog;
import com.apimarketplace.repository.AuditLogRepository;
import com.apimarketplace.security.UserPrincipal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import jakarta.persistence.criteria.Predicate;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public AuditLogResponse record(UserPrincipal principal, String eventType, String targetType, String targetId, String status, String message, String metadata) {
        AuditLog auditLog = new AuditLog();
        if (principal != null) {
            auditLog.setActorId(principal.getId());
            auditLog.setActorEmail(principal.getUsername());
            auditLog.setActorRole(principal.getRole().name());
        }
        auditLog.setEventType(eventType);
        auditLog.setTargetType(normalize(targetType));
        auditLog.setTargetId(normalize(targetId));
        auditLog.setStatus(StringUtils.hasText(status) ? status : "SUCCESS");
        auditLog.setMessage(normalize(message));
        auditLog.setMetadata(normalize(metadata));
        return toResponse(auditLogRepository.save(auditLog));
    }

    public AuditLogResponse recordSystem(String eventType, String targetType, String targetId, String message, String metadata) {
        return record(null, eventType, targetType, targetId, "SUCCESS", message, metadata);
    }

    public List<AuditLogResponse> listAll() {
        return auditLogRepository.findAllByOrderByCreatedAtDesc().stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<AuditLogResponse> listForUser(UUID userId) {
        return auditLogRepository.findByActorIdOrderByCreatedAtDesc(userId).stream().map(this::toResponse).collect(Collectors.toList());
    }

    public Page<AuditLogResponse> pageAuditLogs(
        UserPrincipal principal,
        String actorEmail,
        String actorRole,
        String eventType,
        String targetType,
        String status,
        Instant from,
        Instant to,
        Pageable pageable
    ) {
        Specification<AuditLog> spec = buildSpecification(
            principal,
            actorEmail,
            actorRole,
            eventType,
            targetType,
            status,
            from,
            to
        );
        return auditLogRepository.findAll(spec, pageable).map(this::toResponse);
    }

    private AuditLogResponse toResponse(AuditLog auditLog) {
        return new AuditLogResponse(
            auditLog.getId(),
            auditLog.getActorId(),
            auditLog.getActorEmail(),
            auditLog.getActorRole(),
            auditLog.getEventType(),
            auditLog.getTargetType(),
            auditLog.getTargetId(),
            auditLog.getStatus(),
            auditLog.getMessage(),
            auditLog.getMetadata(),
            auditLog.getCreatedAt(),
            auditLog.getCreatedAt(),
            auditLog.getUpdatedAt()
        );
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private Specification<AuditLog> buildSpecification(
        UserPrincipal principal,
        String actorEmail,
        String actorRole,
        String eventType,
        String targetType,
        String status,
        Instant from,
        Instant to
    ) {
        return (root, query, cb) -> {
            Predicate predicate = cb.conjunction();
            if (principal != null && !principal.getRole().name().equals("ADMIN")) {
                predicate = cb.and(predicate, cb.equal(root.get("actorId"), principal.getId()));
            }
            if (StringUtils.hasText(actorEmail)) {
                predicate = cb.and(predicate, cb.like(cb.lower(root.get("actorEmail")), "%" + actorEmail.trim().toLowerCase() + "%"));
            }
            if (StringUtils.hasText(actorRole)) {
                predicate = cb.and(predicate, cb.equal(cb.lower(root.get("actorRole")), actorRole.trim().toLowerCase()));
            }
            if (StringUtils.hasText(eventType)) {
                predicate = cb.and(predicate, cb.like(cb.lower(root.get("eventType")), "%" + eventType.trim().toLowerCase() + "%"));
            }
            if (StringUtils.hasText(targetType)) {
                predicate = cb.and(predicate, cb.like(cb.lower(root.get("targetType")), "%" + targetType.trim().toLowerCase() + "%"));
            }
            if (StringUtils.hasText(status)) {
                predicate = cb.and(predicate, cb.equal(cb.lower(root.get("status")), status.trim().toLowerCase()));
            }
            if (from != null) {
                predicate = cb.and(predicate, cb.greaterThanOrEqualTo(root.get("createdAt"), from));
            }
            if (to != null) {
                predicate = cb.and(predicate, cb.lessThanOrEqualTo(root.get("createdAt"), to));
            }
            return predicate;
        };
    }
}
