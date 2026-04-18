package com.apimarketplace.dto.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record ApiProductRequest(
    @NotBlank String name,
    String slug,
    @NotBlank String description,
    @NotBlank String category,
    String documentationUrl,
    String openApiSpecUrl,
    @NotNull BigDecimal basePrice,
    Integer requestsPerMinute,
    String region
) {}
