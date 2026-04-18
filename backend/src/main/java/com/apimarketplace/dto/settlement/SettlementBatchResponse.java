package com.apimarketplace.dto.settlement;

import com.apimarketplace.entity.enums.SettlementStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record SettlementBatchResponse(
    UUID id,
    UUID providerId,
    String providerName,
    LocalDate periodStart,
    LocalDate periodEnd,
    int invoiceCount,
    BigDecimal grossRevenue,
    BigDecimal platformFeeRate,
    BigDecimal platformFee,
    BigDecimal taxAmount,
    BigDecimal netPayout,
    SettlementStatus status,
    String payoutReference,
    Instant generatedAt,
    Instant paidAt,
    Instant createdAt,
    Instant updatedAt
) {}
