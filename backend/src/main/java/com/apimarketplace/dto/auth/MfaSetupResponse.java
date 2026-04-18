package com.apimarketplace.dto.auth;

import java.util.List;

public record MfaSetupResponse(
    String secret,
    String qrCodeUrl,
    List<String> backupCodes
) {}
