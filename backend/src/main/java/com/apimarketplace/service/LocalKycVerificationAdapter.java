package com.apimarketplace.service;

import com.apimarketplace.dto.kyc.KycSubmitRequest;
import com.apimarketplace.dto.kyc.KycVerificationOutcome;
import com.apimarketplace.dto.kyc.KycVerificationSummary;
import com.apimarketplace.entity.enums.KycVerificationMethod;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class LocalKycVerificationAdapter implements KycVerificationAdapter {

    private final ComplianceValidationService validationService;

    public LocalKycVerificationAdapter(ComplianceValidationService validationService) {
        this.validationService = validationService;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String providerName() {
        return "LOCAL";
    }

    @Override
    public KycVerificationSummary verify(KycSubmitRequest request) {
        List<KycVerificationOutcome> outcomes = new ArrayList<>();
        for (KycVerificationMethod method : request.verificationMethods()) {
            outcomes.add(verifyMethod(request, method));
        }
        return new KycVerificationSummary(outcomes);
    }

    private KycVerificationOutcome verifyMethod(KycSubmitRequest request, KycVerificationMethod method) {
        boolean verified;
        String message;
        Double score;
        switch (method) {
            case BANK_ACCOUNT -> {
                verified = validationService.isValidBankAccount(request.bankAccountNumber()) && validationService.isValidIfsc(request.bankIfsc());
                message = verified ? "Bank account and IFSC validated locally" : "Bank account details failed local validation";
                score = verified ? 0.99 : 0.12;
            }
            case PAN -> {
                verified = validationService.isValidPan(request.panNumber());
                message = verified ? "PAN format validated locally" : "PAN failed local validation";
                score = verified ? 0.98 : 0.10;
            }
            case GST -> {
                verified = StringUtils.hasText(request.gstin()) && validationService.isValidGstin(request.gstin());
                message = verified ? "GSTIN format validated locally" : "GSTIN failed local validation";
                score = verified ? 0.97 : 0.10;
            }
            case AADHAAR_BASIC -> {
                verified = validationService.isValidAadhaar(request.aadhaarNumber());
                message = verified ? "Basic Aadhaar data validated locally" : "Aadhaar number is missing or invalid";
                score = verified ? 0.95 : 0.08;
            }
            case AADHAAR_OCR -> {
                verified = validationService.isValidAadhaar(request.aadhaarNumber()) && StringUtils.hasText(request.idDocumentImageUrl());
                message = verified ? "Aadhaar OCR inputs validated locally" : "Aadhaar OCR requires Aadhaar number and document image";
                score = verified ? 0.93 : 0.08;
            }
            case DRIVING_LICENSE -> {
                verified = validationService.isValidDrivingLicense(request.drivingLicenseNumber());
                message = verified ? "Driving license format validated locally" : "Driving license failed local validation";
                score = verified ? 0.95 : 0.10;
            }
            case PASSPORT -> {
                verified = validationService.isValidPassport(request.passportNumber());
                message = verified ? "Passport format validated locally" : "Passport failed local validation";
                score = verified ? 0.95 : 0.10;
            }
            case VOTER_ID -> {
                verified = validationService.isValidVoterId(request.voterIdNumber());
                message = verified ? "Voter ID format validated locally" : "Voter ID failed local validation";
                score = verified ? 0.95 : 0.10;
            }
            case FACE_MATCH -> {
                verified = StringUtils.hasText(request.selfieImageUrl()) && StringUtils.hasText(request.idDocumentImageUrl());
                message = verified ? "Face match inputs captured locally" : "Face match requires selfie and ID document images";
                score = verified ? 0.92 : 0.08;
            }
            case FACE_LIVENESS -> {
                verified = StringUtils.hasText(request.selfieImageUrl());
                message = verified ? "Face liveness input captured locally" : "Face liveness requires a selfie image";
                score = verified ? 0.91 : 0.08;
            }
            default -> {
                verified = false;
                message = "Unsupported verification method";
                score = 0.0;
            }
        }

        return new KycVerificationOutcome(
            method,
            verified,
            providerName(),
            "LOCAL-" + method.name() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
            message,
            score
        );
    }
}
