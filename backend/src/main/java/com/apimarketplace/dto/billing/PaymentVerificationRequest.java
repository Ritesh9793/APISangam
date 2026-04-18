package com.apimarketplace.dto.billing;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record PaymentVerificationRequest(
    @NotNull UUID invoiceId,
    @NotBlank String orderId,
    @NotBlank String paymentId,
    @NotBlank String signature
) {}
