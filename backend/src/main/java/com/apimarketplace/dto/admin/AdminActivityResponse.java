package com.apimarketplace.dto.admin;

import java.time.Instant;

public record AdminActivityResponse(
    String category,
    String title,
    String details,
    Instant occurredAt
) {}
