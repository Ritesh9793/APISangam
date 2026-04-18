package com.apimarketplace.dto.billing;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record UsageResponse(
    UUID id,
    UUID subscriptionId,
    String subscriptionName,
    LocalDate usageDate,
    long callCount,
    BigDecimal totalCost,
    Instant createdAt,
    Instant updatedAt
) {}
