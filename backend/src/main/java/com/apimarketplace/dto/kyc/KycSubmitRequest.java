package com.apimarketplace.dto.kyc;

import com.apimarketplace.entity.enums.KycVerificationMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

@Schema(description = "KYC submission payload with method-specific verification inputs for banking, identity, and face checks.")
public record KycSubmitRequest(
    @Schema(example = "Demo Provider Private Limited")
    @NotBlank String legalBusinessName,
    @Schema(example = "Ritesh Sharma")
    String contactName,
    @Schema(example = "provider@apimarketplace.local")
    String email,
    @Schema(example = "9876543210")
    String phoneNumber,
    @Schema(example = "Business")
    @NotBlank String businessType,
    @Schema(example = "ABCDE1234F")
    @NotBlank String panNumber,
    @Schema(example = "22ABCDE1234F1Z5")
    String gstin,
    @Schema(example = "123456789012")
    @NotBlank String bankAccountNumber,
    @Schema(example = "HDFC0ABC123")
    @NotBlank String bankIfsc,
    @Schema(example = "12-digit Aadhaar number")
    String aadhaarNumber,
    @Schema(example = "KA0120200001234")
    String drivingLicenseNumber,
    @Schema(example = "M1234567")
    String passportNumber,
    @Schema(example = "ABC1234567")
    String voterIdNumber,
    @Schema(example = "https://cdn.example.com/selfie.jpg")
    String selfieImageUrl,
    @Schema(example = "https://cdn.example.com/id-document.jpg")
    String idDocumentImageUrl,
    @Schema(description = "Select the verification methods to run for this KYC submission.")
    @NotEmpty List<KycVerificationMethod> verificationMethods,
    @Schema(example = "true")
    boolean consentGiven,
    @Schema(example = "Registered office, Bengaluru")
    String registeredAddress,
    @Schema(description = "Optional supporting document filenames or URLs.")
    List<String> supportingDocuments
) {}
