package com.apimarketplace.dto.notifications;

import com.apimarketplace.entity.enums.NotificationChannel;
import com.apimarketplace.entity.enums.NotificationStatus;
import java.time.Instant;
import java.util.UUID;

public record NotificationEventResponse(
    UUID id,
    UUID userId,
    NotificationChannel channel,
    String recipient,
    String subject,
    String message,
    String eventType,
    NotificationStatus status,
    String provider,
    Instant deliveredAt,
    Instant createdAt,
    Instant updatedAt
) {}
