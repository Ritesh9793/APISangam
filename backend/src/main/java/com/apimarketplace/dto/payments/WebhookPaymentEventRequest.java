package com.apimarketplace.dto.payments;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record WebhookPaymentEventRequest(
    @NotBlank String event,
    @NotNull UUID invoiceId,
    @NotBlank String orderId,
    @NotBlank String paymentId,
    @NotBlank String signature
) {}
