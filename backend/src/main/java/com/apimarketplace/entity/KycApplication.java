package com.apimarketplace.entity;

import com.apimarketplace.entity.enums.KycStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "kyc_application")
public class KycApplication extends BaseEntity {

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "legal_business_name", nullable = false)
    private String legalBusinessName;

    @Column(name = "contact_name")
    private String contactName;

    @Column(nullable = false)
    private String email;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "business_type", nullable = false)
    private String businessType;

    @Column(name = "pan_number")
    private String panNumber;

    @Column(name = "gstin")
    private String gstin;

    @Column(name = "bank_account_masked")
    private String bankAccountMasked;

    @Column(name = "bank_ifsc")
    private String bankIfsc;

    @Column(name = "registered_address", columnDefinition = "text")
    private String registeredAddress;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private KycStatus status = KycStatus.DRAFT;

    @Column(name = "pan_verified", nullable = false)
    private boolean panVerified;

    @Column(name = "gstin_verified", nullable = false)
    private boolean gstinVerified;

    @Column(name = "bank_verified", nullable = false)
    private boolean bankVerified;

    @Column(name = "rejection_reason", columnDefinition = "text")
    private String rejectionReason;

    @Column(name = "reviewer_id")
    private UUID reviewerId;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "supporting_documents_csv", length = 1000)
    private String supportingDocumentsCsv;

    @Column(name = "verification_methods_csv", length = 1000)
    private String verificationMethodsCsv;

    @Column(name = "verification_provider", length = 120)
    private String verificationProvider;

    @Column(name = "verification_reference", length = 120)
    private String verificationReference;

    @Column(name = "verification_summary_text", columnDefinition = "text")
    private String verificationSummaryText;

    @Column(name = "verification_reference_ids_csv", length = 1000)
    private String verificationReferenceIdsCsv;

    @Column(name = "aadhaar_number_masked", length = 32)
    private String aadhaarNumberMasked;

    @Column(name = "aadhaar_basic_verified", nullable = false)
    private boolean aadhaarBasicVerified;

    @Column(name = "aadhaar_ocr_verified", nullable = false)
    private boolean aadhaarOcrVerified;

    @Column(name = "driving_license_masked", length = 32)
    private String drivingLicenseMasked;

    @Column(name = "driving_license_verified", nullable = false)
    private boolean drivingLicenseVerified;

    @Column(name = "passport_masked", length = 32)
    private String passportMasked;

    @Column(name = "passport_verified", nullable = false)
    private boolean passportVerified;

    @Column(name = "voter_id_masked", length = 32)
    private String voterIdMasked;

    @Column(name = "voter_id_verified", nullable = false)
    private boolean voterIdVerified;

    @Column(name = "face_match_verified", nullable = false)
    private boolean faceMatchVerified;

    @Column(name = "face_liveness_verified", nullable = false)
    private boolean faceLivenessVerified;

    @Column(name = "selfie_image_url", length = 500)
    private String selfieImageUrl;

    @Column(name = "id_document_image_url", length = 500)
    private String idDocumentImageUrl;

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getLegalBusinessName() {
        return legalBusinessName;
    }

    public void setLegalBusinessName(String legalBusinessName) {
        this.legalBusinessName = legalBusinessName;
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getBusinessType() {
        return businessType;
    }

    public void setBusinessType(String businessType) {
        this.businessType = businessType;
    }

    public String getPanNumber() {
        return panNumber;
    }

    public void setPanNumber(String panNumber) {
        this.panNumber = panNumber;
    }

    public String getGstin() {
        return gstin;
    }

    public void setGstin(String gstin) {
        this.gstin = gstin;
    }

    public String getBankAccountMasked() {
        return bankAccountMasked;
    }

    public void setBankAccountMasked(String bankAccountMasked) {
        this.bankAccountMasked = bankAccountMasked;
    }

    public String getBankIfsc() {
        return bankIfsc;
    }

    public void setBankIfsc(String bankIfsc) {
        this.bankIfsc = bankIfsc;
    }

    public String getRegisteredAddress() {
        return registeredAddress;
    }

    public void setRegisteredAddress(String registeredAddress) {
        this.registeredAddress = registeredAddress;
    }

    public KycStatus getStatus() {
        return status;
    }

    public void setStatus(KycStatus status) {
        this.status = status;
    }

    public boolean isPanVerified() {
        return panVerified;
    }

    public void setPanVerified(boolean panVerified) {
        this.panVerified = panVerified;
    }

    public boolean isGstinVerified() {
        return gstinVerified;
    }

    public void setGstinVerified(boolean gstinVerified) {
        this.gstinVerified = gstinVerified;
    }

    public boolean isBankVerified() {
        return bankVerified;
    }

    public void setBankVerified(boolean bankVerified) {
        this.bankVerified = bankVerified;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public UUID getReviewerId() {
        return reviewerId;
    }

    public void setReviewerId(UUID reviewerId) {
        this.reviewerId = reviewerId;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(Instant submittedAt) {
        this.submittedAt = submittedAt;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(Instant reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public String getSupportingDocumentsCsv() {
        return supportingDocumentsCsv;
    }

    public void setSupportingDocumentsCsv(String supportingDocumentsCsv) {
        this.supportingDocumentsCsv = supportingDocumentsCsv;
    }

    public String getVerificationMethodsCsv() {
        return verificationMethodsCsv;
    }

    public void setVerificationMethodsCsv(String verificationMethodsCsv) {
        this.verificationMethodsCsv = verificationMethodsCsv;
    }

    public String getVerificationProvider() {
        return verificationProvider;
    }

    public void setVerificationProvider(String verificationProvider) {
        this.verificationProvider = verificationProvider;
    }

    public String getVerificationReference() {
        return verificationReference;
    }

    public void setVerificationReference(String verificationReference) {
        this.verificationReference = verificationReference;
    }

    public String getVerificationSummaryText() {
        return verificationSummaryText;
    }

    public void setVerificationSummaryText(String verificationSummaryText) {
        this.verificationSummaryText = verificationSummaryText;
    }

    public String getVerificationReferenceIdsCsv() {
        return verificationReferenceIdsCsv;
    }

    public void setVerificationReferenceIdsCsv(String verificationReferenceIdsCsv) {
        this.verificationReferenceIdsCsv = verificationReferenceIdsCsv;
    }

    public String getAadhaarNumberMasked() {
        return aadhaarNumberMasked;
    }

    public void setAadhaarNumberMasked(String aadhaarNumberMasked) {
        this.aadhaarNumberMasked = aadhaarNumberMasked;
    }

    public boolean isAadhaarBasicVerified() {
        return aadhaarBasicVerified;
    }

    public void setAadhaarBasicVerified(boolean aadhaarBasicVerified) {
        this.aadhaarBasicVerified = aadhaarBasicVerified;
    }

    public boolean isAadhaarOcrVerified() {
        return aadhaarOcrVerified;
    }

    public void setAadhaarOcrVerified(boolean aadhaarOcrVerified) {
        this.aadhaarOcrVerified = aadhaarOcrVerified;
    }

    public String getDrivingLicenseMasked() {
        return drivingLicenseMasked;
    }

    public void setDrivingLicenseMasked(String drivingLicenseMasked) {
        this.drivingLicenseMasked = drivingLicenseMasked;
    }

    public boolean isDrivingLicenseVerified() {
        return drivingLicenseVerified;
    }

    public void setDrivingLicenseVerified(boolean drivingLicenseVerified) {
        this.drivingLicenseVerified = drivingLicenseVerified;
    }

    public String getPassportMasked() {
        return passportMasked;
    }

    public void setPassportMasked(String passportMasked) {
        this.passportMasked = passportMasked;
    }

    public boolean isPassportVerified() {
        return passportVerified;
    }

    public void setPassportVerified(boolean passportVerified) {
        this.passportVerified = passportVerified;
    }

    public String getVoterIdMasked() {
        return voterIdMasked;
    }

    public void setVoterIdMasked(String voterIdMasked) {
        this.voterIdMasked = voterIdMasked;
    }

    public boolean isVoterIdVerified() {
        return voterIdVerified;
    }

    public void setVoterIdVerified(boolean voterIdVerified) {
        this.voterIdVerified = voterIdVerified;
    }

    public boolean isFaceMatchVerified() {
        return faceMatchVerified;
    }

    public void setFaceMatchVerified(boolean faceMatchVerified) {
        this.faceMatchVerified = faceMatchVerified;
    }

    public boolean isFaceLivenessVerified() {
        return faceLivenessVerified;
    }

    public void setFaceLivenessVerified(boolean faceLivenessVerified) {
        this.faceLivenessVerified = faceLivenessVerified;
    }

    public String getSelfieImageUrl() {
        return selfieImageUrl;
    }

    public void setSelfieImageUrl(String selfieImageUrl) {
        this.selfieImageUrl = selfieImageUrl;
    }

    public String getIdDocumentImageUrl() {
        return idDocumentImageUrl;
    }

    public void setIdDocumentImageUrl(String idDocumentImageUrl) {
        this.idDocumentImageUrl = idDocumentImageUrl;
    }
}
