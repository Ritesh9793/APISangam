package com.apimarketplace.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record MfaVerifyRequest(
    @NotBlank String code
) {}
