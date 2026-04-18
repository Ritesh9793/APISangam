package com.apimarketplace.dto.billing;

import java.util.UUID;

public record PaymentOrderResponse(
    String orderId,
    String currency,
    long amountInPaise,
    String upiIntent,
    UUID invoiceId
) {}
