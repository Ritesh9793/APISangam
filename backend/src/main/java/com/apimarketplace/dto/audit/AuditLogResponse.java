package com.apimarketplace.dto.audit;

import java.time.Instant;
import java.util.UUID;

public record AuditLogResponse(
    UUID id,
    UUID actorId,
    String actorEmail,
    String actorRole,
    String eventType,
    String targetType,
    String targetId,
    String status,
    String message,
    String metadata,
    Instant occurredAt,
    Instant createdAt,
    Instant updatedAt
) {}
