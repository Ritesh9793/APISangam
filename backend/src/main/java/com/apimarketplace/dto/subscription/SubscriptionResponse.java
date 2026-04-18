package com.apimarketplace.dto.subscription;

import com.apimarketplace.entity.enums.SubscriptionStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SubscriptionResponse(
    UUID id,
    UUID consumerId,
    UUID apiProductId,
    String apiProductName,
    String apiProductCategory,
    String planName,
    SubscriptionStatus status,
    BigDecimal monthlyPrice,
    Integer monthlyRequestLimit,
    Instant startedAt,
    Instant cancelledAt,
    Instant createdAt,
    Instant updatedAt
) {}
