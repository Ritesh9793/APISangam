package com.apimarketplace.dto.kyc;

import com.apimarketplace.entity.enums.KycVerificationMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@Schema(description = "Asynchronous provider callback payload for KYC verification updates.")
public record KycProviderCallbackRequest(
    @Schema(description = "Optional KYC application UUID when the provider callback is tied to a known application.", example = "550e8400-e29b-41d4-a716-446655440000")
    UUID applicationId,
    @Schema(description = "Provider-generated verification reference ID.", example = "AADHAARKYC-REF-001")
    String referenceId,
    @Schema(description = "Verification method the callback applies to.", example = "PAN")
    @NotNull KycVerificationMethod method,
    @Schema(description = "Provider status text such as VERIFIED, FAILED, or SUCCESS.", example = "VERIFIED")
    @NotBlank String status,
    @Schema(description = "Whether the provider marked the verification as successful.", example = "true")
    boolean verified,
    @Schema(description = "Optional provider message for audit and review context.", example = "PAN verified by provider")
    String message,
    @Schema(description = "Optional provider confidence score.", example = "0.99")
    Double score,
    @Schema(description = "Optional HMAC signature used to validate the callback payload.", example = "7f0d5bb9b4d0f46ca4d2df91f9d51234")
    String signature
) {}
