package com.apimarketplace.service;

import com.apimarketplace.dto.kyc.KycSubmitRequest;
import com.apimarketplace.dto.kyc.KycVerificationOutcome;
import com.apimarketplace.dto.kyc.KycVerificationSummary;
import com.apimarketplace.entity.enums.KycVerificationMethod;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Service
public class AadhaarKycVerificationAdapter implements KycVerificationAdapter {

    private static final Logger log = LoggerFactory.getLogger(AadhaarKycVerificationAdapter.class);

    private final RestClient restClient;
    private final boolean enabled;
    private final String baseUrl;
    private final String apiKey;
    private final String bankAccountPath;
    private final String panPath;
    private final String gstPath;
    private final String aadhaarBasicPath;
    private final String aadhaarOcrPath;
    private final String drivingLicensePath;
    private final String passportPath;
    private final String voterIdPath;
    private final String faceMatchPath;
    private final String faceLivenessPath;

    public AadhaarKycVerificationAdapter(
        RestClient.Builder restClientBuilder,
        @Value("${app.kyc.provider.enabled:false}") boolean enabled,
        @Value("${app.kyc.provider.base-url:}") String baseUrl,
        @Value("${app.kyc.provider.api-key:}") String apiKey,
        @Value("${app.kyc.provider.paths.bank-account:/bank-account-verification}") String bankAccountPath,
        @Value("${app.kyc.provider.paths.pan:/pan-verification}") String panPath,
        @Value("${app.kyc.provider.paths.gst:/gst-verification}") String gstPath,
        @Value("${app.kyc.provider.paths.aadhaar-basic:/basic-aadhaar-check}") String aadhaarBasicPath,
        @Value("${app.kyc.provider.paths.aadhaar-ocr:/aadhaar-ocr-check}") String aadhaarOcrPath,
        @Value("${app.kyc.provider.paths.driving-license:/driving-license-verification}") String drivingLicensePath,
        @Value("${app.kyc.provider.paths.passport:/passport-verification}") String passportPath,
        @Value("${app.kyc.provider.paths.voter-id:/voter-id-verification}") String voterIdPath,
        @Value("${app.kyc.provider.paths.face-match:/face-match-api}") String faceMatchPath,
        @Value("${app.kyc.provider.paths.face-liveness:/face-liveness-check-api}") String faceLivenessPath
    ) {
        this.restClient = restClientBuilder.build();
        this.enabled = enabled;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.bankAccountPath = bankAccountPath;
        this.panPath = panPath;
        this.gstPath = gstPath;
        this.aadhaarBasicPath = aadhaarBasicPath;
        this.aadhaarOcrPath = aadhaarOcrPath;
        this.drivingLicensePath = drivingLicensePath;
        this.passportPath = passportPath;
        this.voterIdPath = voterIdPath;
        this.faceMatchPath = faceMatchPath;
        this.faceLivenessPath = faceLivenessPath;
    }

    @Override
    public boolean isAvailable() {
        return enabled && StringUtils.hasText(baseUrl);
    }

    @Override
    public String providerName() {
        return "AADHAARKYC";
    }

    @Override
    public KycVerificationSummary verify(KycSubmitRequest request) {
        if (!isAvailable()) {
            throw new IllegalStateException("KYC provider is not configured");
        }
        List<KycVerificationOutcome> outcomes = new ArrayList<>();
        for (KycVerificationMethod method : request.verificationMethods()) {
            outcomes.add(callProvider(request, method));
        }
        return new KycVerificationSummary(outcomes);
    }

    private KycVerificationOutcome callProvider(KycSubmitRequest request, KycVerificationMethod method) {
        try {
            String path = resolvePath(method);
            Map<String, Object> payload = buildPayload(request, method);

            ProviderResponse response = restClient.post()
                .uri(baseUrl + path)
                .contentType(MediaType.APPLICATION_JSON)
                .headers(headers -> {
                    if (StringUtils.hasText(apiKey)) {
                        headers.setBearerAuth(apiKey);
                    }
                    headers.add("X-Provider", providerName());
                    headers.add("X-Verification-Method", method.name());
                })
                .body(payload)
                .retrieve()
                .body(ProviderResponse.class);

            if (response == null) {
                throw new IllegalStateException("Empty provider response");
            }

            return new KycVerificationOutcome(
                method,
                response.verified(),
                providerName(),
                response.referenceId(),
                response.message() == null ? "Verified by provider" : response.message(),
                response.score()
            );
        } catch (Exception ex) {
            log.warn("Remote KYC verification failed for {}: {}", method, ex.getMessage());
            throw ex;
        }
    }

    private String resolvePath(KycVerificationMethod method) {
        return switch (method) {
            case BANK_ACCOUNT -> bankAccountPath;
            case PAN -> panPath;
            case GST -> gstPath;
            case AADHAAR_BASIC -> aadhaarBasicPath;
            case AADHAAR_OCR -> aadhaarOcrPath;
            case DRIVING_LICENSE -> drivingLicensePath;
            case PASSPORT -> passportPath;
            case VOTER_ID -> voterIdPath;
            case FACE_MATCH -> faceMatchPath;
            case FACE_LIVENESS -> faceLivenessPath;
        };
    }

    private Map<String, Object> buildPayload(KycSubmitRequest request, KycVerificationMethod method) {
        Map<String, Object> payload = new HashMap<>();
        putIfPresent(payload, "method", method.name());
        putIfPresent(payload, "legalBusinessName", request.legalBusinessName());
        putIfPresent(payload, "contactName", request.contactName());
        putIfPresent(payload, "email", request.email());
        putIfPresent(payload, "phoneNumber", request.phoneNumber());
        putIfPresent(payload, "consentGiven", request.consentGiven());

        switch (method) {
            case BANK_ACCOUNT -> {
                putIfPresent(payload, "account_number", request.bankAccountNumber());
                putIfPresent(payload, "ifsc", request.bankIfsc());
                putIfPresent(payload, "bankAccountNumber", request.bankAccountNumber());
                putIfPresent(payload, "bankIfsc", request.bankIfsc());
            }
            case PAN -> {
                putIfPresent(payload, "pan", request.panNumber());
                putIfPresent(payload, "panNumber", request.panNumber());
            }
            case GST -> {
                putIfPresent(payload, "gstin", request.gstin());
            }
            case AADHAAR_BASIC -> {
                putIfPresent(payload, "aadhaar_number", request.aadhaarNumber());
                putIfPresent(payload, "aadhaarNumber", request.aadhaarNumber());
            }
            case AADHAAR_OCR -> {
                putIfPresent(payload, "aadhaar_number", request.aadhaarNumber());
                putIfPresent(payload, "aadhaarNumber", request.aadhaarNumber());
                putIfPresent(payload, "document_url", request.idDocumentImageUrl());
                putIfPresent(payload, "documentUrl", request.idDocumentImageUrl());
            }
            case DRIVING_LICENSE -> {
                putIfPresent(payload, "driving_license_number", request.drivingLicenseNumber());
                putIfPresent(payload, "drivingLicenseNumber", request.drivingLicenseNumber());
            }
            case PASSPORT -> {
                putIfPresent(payload, "passport_number", request.passportNumber());
                putIfPresent(payload, "passportNumber", request.passportNumber());
            }
            case VOTER_ID -> {
                putIfPresent(payload, "voter_id_number", request.voterIdNumber());
                putIfPresent(payload, "voterIdNumber", request.voterIdNumber());
            }
            case FACE_MATCH -> {
                putIfPresent(payload, "selfie_url", request.selfieImageUrl());
                putIfPresent(payload, "selfieUrl", request.selfieImageUrl());
                putIfPresent(payload, "document_url", request.idDocumentImageUrl());
                putIfPresent(payload, "documentUrl", request.idDocumentImageUrl());
            }
            case FACE_LIVENESS -> {
                putIfPresent(payload, "selfie_url", request.selfieImageUrl());
                putIfPresent(payload, "selfieUrl", request.selfieImageUrl());
            }
        }

        return payload;
    }

    private void putIfPresent(Map<String, Object> payload, String key, Object value) {
        if (value instanceof String text) {
            if (StringUtils.hasText(text)) {
                payload.put(key, text);
            }
            return;
        }
        if (value != null) {
            payload.put(key, value);
        }
    }

    private record ProviderResponse(boolean verified, String referenceId, String message, Double score) {}
}
