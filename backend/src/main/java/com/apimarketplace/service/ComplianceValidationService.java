package com.apimarketplace.service;

import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ComplianceValidationService {

    public boolean isValidPan(String pan) {
        return StringUtils.hasText(pan) && pan.trim().toUpperCase(Locale.ROOT).matches("^[A-Z]{5}[0-9]{4}[A-Z]{1}$");
    }

    public boolean isValidGstin(String gstin) {
        return StringUtils.hasText(gstin) && gstin.trim().toUpperCase(Locale.ROOT).matches("^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$");
    }

    public boolean isValidIfsc(String ifsc) {
        return StringUtils.hasText(ifsc) && ifsc.trim().toUpperCase(Locale.ROOT).matches("^[A-Z]{4}0[A-Z0-9]{6}$");
    }

    public boolean isValidPhone(String phoneNumber) {
        return !StringUtils.hasText(phoneNumber) || phoneNumber.trim().matches("^[0-9]{10,15}$");
    }

    public boolean isValidBankAccount(String accountNumber) {
        return StringUtils.hasText(accountNumber) && accountNumber.trim().matches("^[0-9]{9,18}$");
    }

    public boolean isValidAadhaar(String aadhaarNumber) {
        return StringUtils.hasText(aadhaarNumber) && aadhaarNumber.trim().matches("^[0-9]{12}$");
    }

    public boolean isValidDrivingLicense(String drivingLicenseNumber) {
        return StringUtils.hasText(drivingLicenseNumber) && drivingLicenseNumber.trim().toUpperCase(Locale.ROOT).matches("^[A-Z]{2}[0-9]{2}[0-9]{11,13}$");
    }

    public boolean isValidPassport(String passportNumber) {
        return StringUtils.hasText(passportNumber) && passportNumber.trim().toUpperCase(Locale.ROOT).matches("^[A-Z][0-9]{7}$");
    }

    public boolean isValidVoterId(String voterId) {
        return StringUtils.hasText(voterId) && voterId.trim().toUpperCase(Locale.ROOT).matches("^[A-Z]{3}[0-9]{7}$");
    }

    public String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    public String normalizeUpper(String value) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : null;
    }

    public String maskAccountNumber(String accountNumber) {
        String normalized = normalize(accountNumber);
        if (!StringUtils.hasText(normalized) || normalized.length() <= 4) {
            return normalized;
        }
        return "****" + normalized.substring(normalized.length() - 4);
    }

    public String maskIdentifier(String identifier) {
        String normalized = normalizeUpper(identifier);
        if (!StringUtils.hasText(normalized) || normalized.length() <= 4) {
            return normalized;
        }
        return "****" + normalized.substring(normalized.length() - 4);
    }
}
