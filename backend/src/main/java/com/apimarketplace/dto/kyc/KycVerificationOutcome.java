package com.apimarketplace.dto.kyc;

import com.apimarketplace.entity.enums.KycVerificationMethod;

public record KycVerificationOutcome(
    KycVerificationMethod method,
    boolean verified,
    String provider,
    String referenceId,
    String message,
    Double score
) {}
