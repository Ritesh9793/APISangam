package com.apimarketplace.dto.subscription;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record SubscribeRequest(
    @NotNull UUID apiProductId,
    @NotBlank String planName
) {}
