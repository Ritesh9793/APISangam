package com.apimarketplace.dto.metering;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ApiCallLogResponse(
    UUID id,
    boolean allowed,
    int statusCode,
    long remainingRequests,
    UUID subscriptionId,
    UUID apiProductId,
    BigDecimal requestCost,
    Instant occurredAt
) {}
