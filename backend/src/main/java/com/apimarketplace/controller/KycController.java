package com.apimarketplace.controller;

import com.apimarketplace.dto.kyc.KycApplicationResponse;
import com.apimarketplace.dto.kyc.KycProviderCallbackRequest;
import com.apimarketplace.dto.kyc.KycReviewRequest;
import com.apimarketplace.dto.kyc.KycSubmitRequest;
import com.apimarketplace.security.UserPrincipal;
import com.apimarketplace.service.KycService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/compliance/kyc")
@Tag(name = "KYC", description = "KYC submission, verification, and admin review endpoints")
public class KycController {

    private final KycService kycService;

    public KycController(KycService kycService) {
        this.kycService = kycService;
    }

    @PostMapping("/submit")
    @Operation(
        summary = "Submit KYC",
        description = """
            Submits a richer compliance package for automated verification.
            Example methods: BANK_ACCOUNT, PAN, GST, AADHAAR_BASIC, AADHAAR_OCR, DRIVING_LICENSE, PASSPORT, VOTER_ID, FACE_MATCH, FACE_LIVENESS.
            """
    )
    public KycApplicationResponse submit(
        @AuthenticationPrincipal UserPrincipal principal,
        @Valid @RequestBody KycSubmitRequest request
    ) {
        return kycService.submit(principal, request);
    }

    @GetMapping("/me")
    @Operation(summary = "Get my KYC", description = "Returns the current user's KYC application and verification status.")
    public KycApplicationResponse mine(@AuthenticationPrincipal UserPrincipal principal) {
        return kycService.getMine(principal);
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List pending KYC applications", description = "Admin queue of KYC applications awaiting review.")
    public List<KycApplicationResponse> pending() {
        return kycService.listPending();
    }

    @PostMapping("/provider/webhook")
    @Operation(summary = "Handle KYC provider callback", description = "Public callback endpoint for asynchronous AadhaarKYC verification updates.")
    public KycApplicationResponse providerWebhook(@Valid @RequestBody KycProviderCallbackRequest request) {
        return kycService.handleProviderCallback(request);
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Approve KYC", description = "Marks the KYC application as approved after review.")
    public KycApplicationResponse approve(
        @AuthenticationPrincipal UserPrincipal reviewer,
        @Parameter(description = "KYC application UUID")
        @PathVariable UUID id
    ) {
        return kycService.approve(id, reviewer);
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reject KYC", description = "Rejects the application with an optional review reason.")
    public KycApplicationResponse reject(
        @AuthenticationPrincipal UserPrincipal reviewer,
        @Parameter(description = "KYC application UUID")
        @PathVariable UUID id,
        @RequestBody(required = false) KycReviewRequest request
    ) {
        return kycService.reject(id, reviewer, request);
    }
}
