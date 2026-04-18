package com.apimarketplace.dto.auth;

import com.apimarketplace.entity.enums.KycStatus;
import com.apimarketplace.entity.enums.UserRole;
import java.util.UUID;

public record UserSummaryResponse(
    UUID id,
    String email,
    String fullName,
    String companyName,
    UserRole role,
    boolean mfaEnabled,
    KycStatus kycStatus
) {}
