package com.apimarketplace.service;

import com.apimarketplace.dto.kyc.KycSubmitRequest;
import com.apimarketplace.dto.kyc.KycVerificationSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class KycVerificationGateway {

    private static final Logger log = LoggerFactory.getLogger(KycVerificationGateway.class);

    private final AadhaarKycVerificationAdapter remoteAdapter;
    private final LocalKycVerificationAdapter localAdapter;
    private final boolean providerPreferred;

    public KycVerificationGateway(
        AadhaarKycVerificationAdapter remoteAdapter,
        LocalKycVerificationAdapter localAdapter,
        @Value("${app.kyc.provider.prefer-remote:true}") boolean providerPreferred
    ) {
        this.remoteAdapter = remoteAdapter;
        this.localAdapter = localAdapter;
        this.providerPreferred = providerPreferred;
    }

    public KycVerificationSummary verify(KycSubmitRequest request) {
        if (providerPreferred && remoteAdapter.isAvailable()) {
            try {
                return remoteAdapter.verify(request);
            } catch (Exception ex) {
                log.warn("Falling back to local KYC verification: {}", ex.getMessage());
            }
        }
        return localAdapter.verify(request);
    }

    public String providerName() {
        return providerPreferred && remoteAdapter.isAvailable() ? remoteAdapter.providerName() : localAdapter.providerName();
    }
}
