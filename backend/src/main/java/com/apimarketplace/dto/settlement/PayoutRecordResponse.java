package com.apimarketplace.dto.settlement;

import com.apimarketplace.entity.enums.PayoutStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PayoutRecordResponse(
    UUID id,
    UUID settlementBatchId,
    UUID providerId,
    String providerName,
    BigDecimal amount,
    String payoutMode,
    PayoutStatus status,
    String providerReference,
    Instant processedAt,
    Instant createdAt,
    Instant updatedAt
) {}
