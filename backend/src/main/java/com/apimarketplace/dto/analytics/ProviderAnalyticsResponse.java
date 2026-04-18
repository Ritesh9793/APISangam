package com.apimarketplace.dto.analytics;

import java.math.BigDecimal;
import java.util.List;

public record ProviderAnalyticsResponse(
    int totalApis,
    int activeApis,
    int totalSubscriptions,
    int activeSubscriptions,
    long totalCalls,
    BigDecimal totalRevenue,
    BigDecimal pendingRevenue,
    List<ApiPerformanceSummary> apiPerformance
) {}
