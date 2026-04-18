package com.apimarketplace.dto.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ApiProductResponse(
    UUID id,
    UUID providerId,
    String providerName,
    String name,
    String slug,
    String description,
    String category,
    String documentationUrl,
    String openApiSpecUrl,
    BigDecimal basePrice,
    Integer requestsPerMinute,
    boolean active,
    String region,
    Instant createdAt,
    Instant updatedAt
) {}
