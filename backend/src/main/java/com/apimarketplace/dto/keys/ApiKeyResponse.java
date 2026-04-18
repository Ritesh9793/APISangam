package com.apimarketplace.dto.keys;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ApiKeyResponse(
    UUID id,
    UUID subscriptionId,
    UUID apiProductId,
    String label,
    List<String> scopes,
    boolean active,
    Instant expiresAt,
    Instant lastUsedAt,
    Instant createdAt,
    Instant updatedAt,
    String plainKey
) {}
