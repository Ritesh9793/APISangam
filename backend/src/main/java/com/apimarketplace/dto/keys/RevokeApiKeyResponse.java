package com.apimarketplace.dto.keys;

import java.util.UUID;

public record RevokeApiKeyResponse(
    UUID id,
    boolean active
) {}
