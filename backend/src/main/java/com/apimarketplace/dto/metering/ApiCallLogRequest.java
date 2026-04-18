package com.apimarketplace.dto.metering;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record ApiCallLogRequest(
    @NotBlank String apiKey,
    @NotBlank String httpMethod,
    @NotBlank String requestPath,
    @NotNull Integer statusCode,
    @Min(0) long latencyMs,
    BigDecimal requestCost,
    String ipAddress,
    String userAgent
) {}
