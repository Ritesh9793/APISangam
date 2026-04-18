package com.apimarketplace.dto.auth;

public record AuthResponse(
    String accessToken,
    String refreshToken,
    String tokenType,
    long expiresInSeconds,
    long refreshExpiresInSeconds,
    UserSummaryResponse user,
    boolean mfaEnabled
) {}
