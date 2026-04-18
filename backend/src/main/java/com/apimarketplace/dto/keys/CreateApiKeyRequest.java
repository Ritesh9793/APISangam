package com.apimarketplace.dto.keys;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record CreateApiKeyRequest(
    @NotNull UUID subscriptionId,
    @NotBlank String label,
    List<String> scopes,
    Integer expiresInDays
) {}
