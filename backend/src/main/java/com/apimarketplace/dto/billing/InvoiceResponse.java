package com.apimarketplace.dto.billing;

import com.apimarketplace.entity.enums.InvoiceStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record InvoiceResponse(
    UUID id,
    UUID subscriptionId,
    String subscriptionName,
    String invoiceNumber,
    InvoiceStatus status,
    LocalDate billingPeriodStart,
    LocalDate billingPeriodEnd,
    BigDecimal baseAmount,
    BigDecimal taxAmount,
    BigDecimal totalAmount,
    Instant dueAt,
    Instant paidAt,
    String paymentProvider,
    String paymentReference,
    Instant createdAt,
    Instant updatedAt
) {}
