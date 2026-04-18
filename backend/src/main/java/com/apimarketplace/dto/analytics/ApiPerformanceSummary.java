package com.apimarketplace.dto.analytics;

import java.math.BigDecimal;
import java.util.UUID;

public record ApiPerformanceSummary(
    UUID apiProductId,
    String apiName,
    String slug,
    long subscriptionCount,
    long callCount,
    BigDecimal revenue
) {}
