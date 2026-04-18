package com.apimarketplace.service;

import com.apimarketplace.dto.kyc.KycSubmitRequest;
import com.apimarketplace.dto.kyc.KycVerificationSummary;

public interface KycVerificationAdapter {

    boolean isAvailable();

    String providerName();

    KycVerificationSummary verify(KycSubmitRequest request);
}
