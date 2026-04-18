package com.apimarketplace.dto.billing;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreatePaymentOrderRequest(
    @NotNull UUID invoiceId,
    String currency
) {}
