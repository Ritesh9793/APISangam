package com.apimarketplace.service;

import com.apimarketplace.dto.kyc.KycApplicationResponse;
import com.apimarketplace.dto.kyc.KycProviderCallbackRequest;
import com.apimarketplace.dto.kyc.KycReviewRequest;
import com.apimarketplace.dto.kyc.KycSubmitRequest;
import com.apimarketplace.dto.kyc.KycVerificationSummary;
import com.apimarketplace.entity.KycApplication;
import com.apimarketplace.entity.enums.KycStatus;
import com.apimarketplace.entity.enums.KycVerificationMethod;
import com.apimarketplace.entity.enums.UserRole;
import com.apimarketplace.exception.ApiException;
import com.apimarketplace.repository.KycApplicationRepository;
import com.apimarketplace.security.UserPrincipal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class KycService {

    private final KycApplicationRepository kycApplicationRepository;
    private final ComplianceValidationService validationService;
    private final KycVerificationGateway verificationGateway;
    private final AuditService auditService;
    private final NotificationService notificationService;
    private final String providerWebhookSecret;

    public KycService(
        KycApplicationRepository kycApplicationRepository,
        ComplianceValidationService validationService,
        KycVerificationGateway verificationGateway,
        AuditService auditService,
        NotificationService notificationService,
        @Value("${app.kyc.provider.webhook-secret:}") String providerWebhookSecret
    ) {
        this.kycApplicationRepository = kycApplicationRepository;
        this.validationService = validationService;
        this.verificationGateway = verificationGateway;
        this.auditService = auditService;
        this.notificationService = notificationService;
        this.providerWebhookSecret = providerWebhookSecret;
    }

    @Transactional
    public KycApplicationResponse submit(UserPrincipal principal, KycSubmitRequest request) {
        validateSubmission(principal, request);
        KycVerificationSummary verificationSummary = verificationGateway.verify(request);
        KycApplication application = kycApplicationRepository.findByUserId(principal.getId()).orElseGet(KycApplication::new);
        populateApplication(application, principal, request, verificationSummary);
        application.setStatus(verificationSummary.allVerified() ? KycStatus.SUBMITTED : KycStatus.UNDER_REVIEW);
        application.setSubmittedAt(Instant.now());
        application.setReviewedAt(null);
        application.setReviewerId(null);
        application.setRejectionReason(null);
        KycApplicationResponse response = toResponse(kycApplicationRepository.save(application));
        auditService.record(principal, "KYC_SUBMITTED", "kyc", response.id() == null ? null : response.id().toString(), "SUCCESS", "KYC application submitted", response.legalBusinessName());
        notificationService.sendEmail(principal.getId(), principal.getUsername(), "KYC application submitted", "Your KYC application is under review.", "KYC_SUBMITTED");
        if (StringUtils.hasText(response.phoneNumber())) {
            notificationService.sendSms(principal.getId(), response.phoneNumber(), "Your KYC application has been submitted and is under review.", "KYC_SUBMITTED");
        }
        return response;
    }

    public KycApplicationResponse getMine(UserPrincipal principal) {
        return kycApplicationRepository.findByUserId(principal.getId())
            .map(this::toResponse)
            .orElseGet(() -> emptyDraft(principal));
    }

    public List<KycApplicationResponse> listPending() {
        return kycApplicationRepository.findByStatusInOrderByUpdatedAtDesc(List.of(KycStatus.SUBMITTED, KycStatus.UNDER_REVIEW, KycStatus.NEEDS_MORE_INFO))
            .stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public KycApplicationResponse approve(UUID applicationId, UserPrincipal reviewer) {
        requireAdmin(reviewer);
        KycApplication application = load(applicationId);
        application.setStatus(KycStatus.APPROVED);
        application.setReviewerId(reviewer.getId());
        application.setReviewedAt(Instant.now());
        application.setRejectionReason(null);
        KycApplicationResponse response = toResponse(kycApplicationRepository.save(application));
        auditService.record(reviewer, "KYC_APPROVED", "kyc", applicationId.toString(), "SUCCESS", "KYC approved", response.userId().toString());
        notificationService.sendEmail(response.userId(), response.email(), "KYC approved", "Your KYC application has been approved.", "KYC_APPROVED");
        if (StringUtils.hasText(response.phoneNumber())) {
            notificationService.sendSms(response.userId(), response.phoneNumber(), "Your KYC application has been approved.", "KYC_APPROVED");
        }
        return response;
    }

    @Transactional
    public KycApplicationResponse reject(UUID applicationId, UserPrincipal reviewer, KycReviewRequest request) {
        requireAdmin(reviewer);
        KycApplication application = load(applicationId);
        application.setStatus(KycStatus.REJECTED);
        application.setReviewerId(reviewer.getId());
        application.setReviewedAt(Instant.now());
        application.setRejectionReason(validationService.normalize(request == null ? null : request.reason()));
        KycApplicationResponse response = toResponse(kycApplicationRepository.save(application));
        auditService.record(reviewer, "KYC_REJECTED", "kyc", applicationId.toString(), "SUCCESS", "KYC rejected", response.userId().toString());
        notificationService.sendEmail(response.userId(), response.email(), "KYC rejected", "Your KYC application was rejected." + (response.rejectionReason() == null ? "" : " Reason: " + response.rejectionReason()), "KYC_REJECTED");
        if (StringUtils.hasText(response.phoneNumber())) {
            notificationService.sendSms(response.userId(), response.phoneNumber(), "Your KYC application was rejected.", "KYC_REJECTED");
        }
        return response;
    }

    @Transactional
    public KycApplicationResponse handleProviderCallback(KycProviderCallbackRequest request) {
        verifyProviderCallbackSignature(request);
        KycApplication application = request.applicationId() != null
            ? load(request.applicationId())
            : loadByReference(request.referenceId());

        applyCallbackOutcome(application, request);
        KycApplicationResponse response = toResponse(kycApplicationRepository.save(application));
        auditService.recordSystem(
            "KYC_PROVIDER_CALLBACK",
            "kyc",
            application.getId().toString(),
            "KYC provider callback processed",
            request.method().name() + ":" + request.status()
        );
        notificationService.sendEmail(
            response.userId(),
            response.email(),
            "KYC verification updated",
            "Your " + request.method().name() + " verification status is now " + request.status() + ".",
            "KYC_PROVIDER_CALLBACK"
        );
        return response;
    }

    public void requireApproved(UUID userId, String actionDescription) {
        if (!isApproved(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "KYC approval is required to " + actionDescription);
        }
    }

    public boolean isApproved(UUID userId) {
        return kycApplicationRepository.findByUserId(userId)
            .map(application -> application.getStatus() == KycStatus.APPROVED)
            .orElse(false);
    }

    public KycStatus getStatus(UUID userId) {
        return kycApplicationRepository.findByUserId(userId)
            .map(KycApplication::getStatus)
            .orElse(KycStatus.DRAFT);
    }

    public void requireApproved(UserPrincipal principal, String actionDescription) {
        if (principal.getRole() == UserRole.ADMIN) {
            return;
        }
        requireApproved(principal.getId(), actionDescription);
    }

    private void validateSubmission(UserPrincipal principal, KycSubmitRequest request) {
        if (request == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "KYC submission payload is required");
        }
        if (!request.consentGiven()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Consent is required to submit KYC");
        }
        if (request.verificationMethods() == null || request.verificationMethods().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "At least one verification method is required");
        }
        if (!validationService.isValidPan(request.panNumber())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid PAN format");
        }
        if (StringUtils.hasText(request.gstin()) && !validationService.isValidGstin(request.gstin())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid GSTIN format");
        }
        if (!validationService.isValidIfsc(request.bankIfsc())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid IFSC format");
        }
        if (!validationService.isValidBankAccount(request.bankAccountNumber())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid bank account number");
        }
        if (!validationService.isValidPhone(request.phoneNumber())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid phone number");
        }
        if (StringUtils.hasText(request.email()) && !request.email().trim().equalsIgnoreCase(principal.getUsername())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "KYC email must match the account email");
        }
        validateMethodSpecificFields(request);
    }

    private void populateApplication(KycApplication application, UserPrincipal principal, KycSubmitRequest request, KycVerificationSummary verificationSummary) {
        application.setUserId(principal.getId());
        application.setLegalBusinessName(validationService.normalize(request.legalBusinessName()));
        application.setContactName(validationService.normalize(request.contactName()));
        application.setEmail(principal.getUsername());
        application.setPhoneNumber(validationService.normalize(request.phoneNumber()));
        application.setBusinessType(validationService.normalize(request.businessType()));
        application.setPanNumber(validationService.normalizeUpper(request.panNumber()));
        application.setGstin(validationService.normalizeUpper(request.gstin()));
        application.setBankAccountMasked(validationService.maskAccountNumber(request.bankAccountNumber()));
        application.setBankIfsc(validationService.normalizeUpper(request.bankIfsc()));
        application.setRegisteredAddress(validationService.normalize(request.registeredAddress()));
        application.setPanVerified(hasOutcome(verificationSummary, KycVerificationMethod.PAN));
        application.setGstinVerified(hasOutcome(verificationSummary, KycVerificationMethod.GST));
        application.setBankVerified(hasOutcome(verificationSummary, KycVerificationMethod.BANK_ACCOUNT));
        application.setAadhaarBasicVerified(hasOutcome(verificationSummary, KycVerificationMethod.AADHAAR_BASIC));
        application.setAadhaarOcrVerified(hasOutcome(verificationSummary, KycVerificationMethod.AADHAAR_OCR));
        application.setDrivingLicenseVerified(hasOutcome(verificationSummary, KycVerificationMethod.DRIVING_LICENSE));
        application.setPassportVerified(hasOutcome(verificationSummary, KycVerificationMethod.PASSPORT));
        application.setVoterIdVerified(hasOutcome(verificationSummary, KycVerificationMethod.VOTER_ID));
        application.setFaceMatchVerified(hasOutcome(verificationSummary, KycVerificationMethod.FACE_MATCH));
        application.setFaceLivenessVerified(hasOutcome(verificationSummary, KycVerificationMethod.FACE_LIVENESS));
        application.setVerificationMethodsCsv(joinMethods(request.verificationMethods()));
        application.setVerificationProvider(verificationSummary.providerLabel());
        application.setVerificationReference(verificationSummary.referenceText());
        application.setVerificationSummaryText(verificationSummary.summaryText());
        application.setVerificationReferenceIdsCsv(verificationSummary.referenceText());
        application.setAadhaarNumberMasked(validationService.maskIdentifier(request.aadhaarNumber()));
        application.setDrivingLicenseMasked(validationService.maskIdentifier(request.drivingLicenseNumber()));
        application.setPassportMasked(validationService.maskIdentifier(request.passportNumber()));
        application.setVoterIdMasked(validationService.maskIdentifier(request.voterIdNumber()));
        application.setSelfieImageUrl(validationService.normalize(request.selfieImageUrl()));
        application.setIdDocumentImageUrl(validationService.normalize(request.idDocumentImageUrl()));
        application.setSupportingDocumentsCsv(joinDocuments(request.supportingDocuments()));
    }

    private void validateMethodSpecificFields(KycSubmitRequest request) {
        for (KycVerificationMethod method : request.verificationMethods()) {
            switch (method) {
                case BANK_ACCOUNT -> {
                    if (!validationService.isValidBankAccount(request.bankAccountNumber()) || !validationService.isValidIfsc(request.bankIfsc())) {
                        throw new ApiException(HttpStatus.BAD_REQUEST, "Bank account verification requires a valid account number and IFSC");
                    }
                }
                case PAN -> {
                    if (!validationService.isValidPan(request.panNumber())) {
                        throw new ApiException(HttpStatus.BAD_REQUEST, "PAN verification requires a valid PAN");
                    }
                }
                case GST -> {
                    if (!StringUtils.hasText(request.gstin()) || !validationService.isValidGstin(request.gstin())) {
                        throw new ApiException(HttpStatus.BAD_REQUEST, "GST verification requires a valid GSTIN");
                    }
                }
                case AADHAAR_BASIC -> {
                    if (!validationService.isValidAadhaar(request.aadhaarNumber())) {
                        throw new ApiException(HttpStatus.BAD_REQUEST, "Basic Aadhaar verification requires a valid Aadhaar number");
                    }
                }
                case AADHAAR_OCR -> {
                    if (!validationService.isValidAadhaar(request.aadhaarNumber()) || !StringUtils.hasText(request.idDocumentImageUrl())) {
                        throw new ApiException(HttpStatus.BAD_REQUEST, "Aadhaar OCR verification requires Aadhaar number and document image URL");
                    }
                }
                case DRIVING_LICENSE -> {
                    if (!validationService.isValidDrivingLicense(request.drivingLicenseNumber())) {
                        throw new ApiException(HttpStatus.BAD_REQUEST, "Driving license verification requires a valid license number");
                    }
                }
                case PASSPORT -> {
                    if (!validationService.isValidPassport(request.passportNumber())) {
                        throw new ApiException(HttpStatus.BAD_REQUEST, "Passport verification requires a valid passport number");
                    }
                }
                case VOTER_ID -> {
                    if (!validationService.isValidVoterId(request.voterIdNumber())) {
                        throw new ApiException(HttpStatus.BAD_REQUEST, "Voter ID verification requires a valid voter ID number");
                    }
                }
                case FACE_MATCH -> {
                    if (!StringUtils.hasText(request.selfieImageUrl()) || !StringUtils.hasText(request.idDocumentImageUrl())) {
                        throw new ApiException(HttpStatus.BAD_REQUEST, "Face match verification requires selfie and ID document image URLs");
                    }
                }
                case FACE_LIVENESS -> {
                    if (!StringUtils.hasText(request.selfieImageUrl())) {
                        throw new ApiException(HttpStatus.BAD_REQUEST, "Face liveness verification requires a selfie image URL");
                    }
                }
            }
        }
    }

    private void applyCallbackOutcome(KycApplication application, KycProviderCallbackRequest request) {
        boolean verified = request.verified() || "VERIFIED".equalsIgnoreCase(request.status()) || "SUCCESS".equalsIgnoreCase(request.status());
        switch (request.method()) {
            case BANK_ACCOUNT -> application.setBankVerified(verified);
            case PAN -> application.setPanVerified(verified);
            case GST -> application.setGstinVerified(verified);
            case AADHAAR_BASIC -> application.setAadhaarBasicVerified(verified);
            case AADHAAR_OCR -> application.setAadhaarOcrVerified(verified);
            case DRIVING_LICENSE -> application.setDrivingLicenseVerified(verified);
            case PASSPORT -> application.setPassportVerified(verified);
            case VOTER_ID -> application.setVoterIdVerified(verified);
            case FACE_MATCH -> application.setFaceMatchVerified(verified);
            case FACE_LIVENESS -> application.setFaceLivenessVerified(verified);
        }
        application.setVerificationProvider("AADHAARKYC");
        if (StringUtils.hasText(request.referenceId())) {
            String references = application.getVerificationReferenceIdsCsv();
            if (!StringUtils.hasText(references)) {
                application.setVerificationReferenceIdsCsv(request.referenceId());
            } else if (!references.contains(request.referenceId())) {
                application.setVerificationReferenceIdsCsv(references + "," + request.referenceId());
            }
            application.setVerificationReference(request.referenceId());
        }
        application.setVerificationSummaryText(buildCallbackSummary(application, request, verified));
        if (application.getStatus() != KycStatus.APPROVED) {
            application.setStatus(verified ? KycStatus.UNDER_REVIEW : KycStatus.NEEDS_MORE_INFO);
        }
    }

    private String buildCallbackSummary(KycApplication application, KycProviderCallbackRequest request, boolean verified) {
        String existing = application.getVerificationSummaryText();
        String update = request.method().name() + "=" + (verified ? "VERIFIED" : "FAILED") + (StringUtils.hasText(request.message()) ? " (" + request.message().trim() + ")" : "");
        if (!StringUtils.hasText(existing)) {
            return update;
        }
        return existing + " | " + update;
    }

    private String joinDocuments(List<String> documents) {
        if (documents == null || documents.isEmpty()) {
            return null;
        }
        return documents.stream()
            .filter(StringUtils::hasText)
            .map(String::trim)
            .reduce((left, right) -> left + "," + right)
            .orElse(null);
    }

    private KycApplicationResponse emptyDraft(UserPrincipal principal) {
        KycApplication application = new KycApplication();
        application.setUserId(principal.getId());
        application.setEmail(principal.getUsername());
        application.setStatus(KycStatus.DRAFT);
        return toResponse(application);
    }

    private KycApplication load(UUID id) {
        return kycApplicationRepository.findById(id)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "KYC application not found"));
    }

    private KycApplication loadByReference(String referenceId) {
        if (!StringUtils.hasText(referenceId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Provider callback must include applicationId or referenceId");
        }
        return kycApplicationRepository.findFirstByVerificationReferenceIdsCsvContaining(referenceId.trim())
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "KYC application not found for provider reference"));
    }

    private void requireAdmin(UserPrincipal principal) {
        if (principal.getRole() != UserRole.ADMIN) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Admin access required");
        }
    }

    private KycApplicationResponse toResponse(KycApplication application) {
        return new KycApplicationResponse(
            application.getId(),
            application.getUserId(),
            application.getLegalBusinessName(),
            application.getContactName(),
            application.getEmail(),
            application.getPhoneNumber(),
            application.getBusinessType(),
            application.getPanNumber(),
            application.getGstin(),
            application.getBankAccountMasked(),
            application.getBankIfsc(),
            application.getRegisteredAddress(),
            application.getStatus(),
            application.isPanVerified(),
            application.isGstinVerified(),
            application.isBankVerified(),
            application.isAadhaarBasicVerified(),
            application.isAadhaarOcrVerified(),
            application.isDrivingLicenseVerified(),
            application.isPassportVerified(),
            application.isVoterIdVerified(),
            application.isFaceMatchVerified(),
            application.isFaceLivenessVerified(),
            application.getRejectionReason(),
            application.getReviewerId(),
            application.getSubmittedAt(),
            application.getReviewedAt(),
            parseMethods(application.getVerificationMethodsCsv()),
            application.getVerificationProvider(),
            application.getVerificationReference(),
            application.getVerificationSummaryText(),
            parseCsv(application.getVerificationReferenceIdsCsv()),
            application.getAadhaarNumberMasked(),
            application.getDrivingLicenseMasked(),
            application.getPassportMasked(),
            application.getVoterIdMasked(),
            application.getSelfieImageUrl(),
            application.getIdDocumentImageUrl(),
            parseCsv(application.getSupportingDocumentsCsv()),
            application.getCreatedAt(),
            application.getUpdatedAt()
        );
    }

    private boolean hasOutcome(KycVerificationSummary summary, KycVerificationMethod method) {
        if (summary == null || summary.outcomes() == null) {
            return false;
        }
        return summary.outcomes().stream().anyMatch(outcome -> outcome.method() == method && outcome.verified());
    }

    private String joinMethods(List<KycVerificationMethod> methods) {
        if (methods == null || methods.isEmpty()) {
            return null;
        }
        return methods.stream().map(Enum::name).collect(Collectors.joining(","));
    }

    private List<KycVerificationMethod> parseMethods(String csv) {
        if (!StringUtils.hasText(csv)) {
            return List.of();
        }
        return java.util.Arrays.stream(csv.split(","))
            .filter(StringUtils::hasText)
            .map(value -> KycVerificationMethod.valueOf(value.trim()))
            .toList();
    }

    private List<String> parseCsv(String csv) {
        if (!StringUtils.hasText(csv)) {
            return List.of();
        }
        return java.util.Arrays.stream(csv.split(","))
            .map(String::trim)
            .filter(StringUtils::hasText)
            .toList();
    }

    private void verifyProviderCallbackSignature(KycProviderCallbackRequest request) {
        if (!StringUtils.hasText(providerWebhookSecret) || !StringUtils.hasText(request.signature())) {
            return;
        }
        try {
            String payload = (request.applicationId() == null ? "" : request.applicationId()) + "|" +
                (request.referenceId() == null ? "" : request.referenceId()) + "|" +
                request.method().name() + "|" + request.status() + "|" + request.verified();
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(providerWebhookSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256"));
            String expected = java.util.HexFormat.of().formatHex(mac.doFinal(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            if (!java.security.MessageDigest.isEqual(expected.getBytes(java.nio.charset.StandardCharsets.UTF_8), request.signature().getBytes(java.nio.charset.StandardCharsets.UTF_8))) {
                throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid KYC provider signature");
            }
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to verify KYC provider signature");
        }
    }
}
