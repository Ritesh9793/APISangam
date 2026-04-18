package com.apimarketplace.dto.billing;

import java.util.UUID;

public record PaymentVerificationResponse(
    boolean verified,
    String paymentReference,
    UUID invoiceId
) {}
