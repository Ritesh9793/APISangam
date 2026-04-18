package com.apimarketplace.service;

import com.apimarketplace.dto.auth.AuthResponse;
import com.apimarketplace.dto.auth.LoginRequest;
import com.apimarketplace.dto.auth.LogoutRequest;
import com.apimarketplace.dto.auth.MfaSetupResponse;
import com.apimarketplace.dto.auth.MfaVerifyRequest;
import com.apimarketplace.dto.auth.RefreshTokenRequest;
import com.apimarketplace.dto.auth.RegisterRequest;
import com.apimarketplace.dto.auth.UserSummaryResponse;
import com.apimarketplace.entity.RefreshTokenSession;
import com.apimarketplace.entity.UserAccount;
import com.apimarketplace.entity.enums.UserRole;
import com.apimarketplace.exception.ApiException;
import com.apimarketplace.repository.RefreshTokenSessionRepository;
import com.apimarketplace.repository.UserRepository;
import com.apimarketplace.security.JwtService;
import com.apimarketplace.security.TotpService;
import com.apimarketplace.security.UserPrincipal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.Base64;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AuthService {

    private static final String ISSUER = "API Marketplace";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final RefreshTokenSessionRepository refreshTokenSessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TotpService totpService;
    private final KycService kycService;
    private final AuditService auditService;
    private final NotificationService notificationService;

    public AuthService(
        UserRepository userRepository,
        RefreshTokenSessionRepository refreshTokenSessionRepository,
        PasswordEncoder passwordEncoder,
        JwtService jwtService,
        TotpService totpService,
        KycService kycService,
        AuditService auditService,
        NotificationService notificationService
    ) {
        this.userRepository = userRepository;
        this.refreshTokenSessionRepository = refreshTokenSessionRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.totpService = totpService;
        this.kycService = kycService;
        this.auditService = auditService;
        this.notificationService = notificationService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request, String ipAddress, String userAgent) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "A user with this email already exists");
        }

        UserAccount account = new UserAccount();
        account.setEmail(email);
        account.setPasswordHash(passwordEncoder.encode(request.password()));
        account.setFullName(request.fullName().trim());
        account.setCompanyName(StringUtils.hasText(request.companyName()) ? request.companyName().trim() : null);
        account.setRole(request.role() == null ? UserRole.CONSUMER : request.role());
        account.setEnabled(true);
        userRepository.save(account);
        auditService.recordSystem("AUTH_REGISTER", "user", account.getId().toString(), "User registered", account.getEmail());
        notificationService.sendEmail(account.getId(), account.getEmail(), "Welcome to API Marketplace", "Your account has been created successfully.", "AUTH_REGISTER");
        return issueSession(account, ipAddress, userAgent);
    }

    @Transactional
    public AuthResponse login(LoginRequest request, String ipAddress, String userAgent) {
        UserAccount account = userRepository.findByEmailIgnoreCase(normalizeEmail(request.email()))
            .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));

        if (!account.isEnabled()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Account is disabled");
        }

        if (!passwordEncoder.matches(request.password(), account.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        if (account.isMfaEnabled()) {
            if (!StringUtils.hasText(request.mfaCode())) {
                throw new ApiException(HttpStatus.UNAUTHORIZED, "MFA code is required");
            }

            if (!verifyMfaCode(account, request.mfaCode())) {
                throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid MFA code");
            }
        }

        auditService.recordSystem("AUTH_LOGIN", "user", account.getId().toString(), "User logged in", account.getEmail());
        return issueSession(account, ipAddress, userAgent);
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request, String ipAddress, String userAgent) {
        RefreshTokenSession session = loadActiveSession(request.refreshToken());
        UserAccount account = loadAccount(session.getUserId());
        if (!account.isEnabled()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Account is disabled");
        }

        String newRefreshToken = createRefreshToken();
        String newRefreshHash = hashToken(newRefreshToken);
        session.setLastUsedAt(Instant.now());
        session.setRevokedAt(Instant.now());
        session.setReplacedByTokenHash(newRefreshHash);
        refreshTokenSessionRepository.save(session);

        RefreshTokenSession replacement = new RefreshTokenSession();
        replacement.setUserId(account.getId());
        replacement.setTokenHash(newRefreshHash);
        replacement.setExpiresAt(Instant.now().plusSeconds(jwtService.getRefreshTokenDays() * 24 * 60 * 60));
        replacement.setLastUsedAt(Instant.now());
        replacement.setIpAddress(ipAddress);
        replacement.setUserAgent(userAgent);
        refreshTokenSessionRepository.save(replacement);

        auditService.recordSystem("AUTH_REFRESH", "user", account.getId().toString(), "Access token refreshed", account.getEmail());
        String accessToken = jwtService.generateAccessToken(account);
        return new AuthResponse(
            accessToken,
            newRefreshToken,
            "Bearer",
            jwtService.getAccessTokenMinutes() * 60,
            jwtService.getRefreshTokenDays() * 24 * 60 * 60,
            toSummary(account),
            account.isMfaEnabled()
        );
    }

    @Transactional
    public void logout(UserPrincipal principal, LogoutRequest request) {
        RefreshTokenSession session = loadActiveSession(request.refreshToken());
        if (!session.getUserId().equals(principal.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Refresh token does not belong to the current user");
        }
        session.setRevokedAt(Instant.now());
        session.setLastUsedAt(Instant.now());
        refreshTokenSessionRepository.save(session);
        auditService.record(principal, "AUTH_LOGOUT", "session", session.getId().toString(), "SUCCESS", "User logged out", session.getIpAddress());
    }

    public MfaSetupResponse setupMfa(UserPrincipal principal) {
        UserAccount account = loadAccount(principal.getId());
        String secret = StringUtils.hasText(account.getMfaSecret()) ? account.getMfaSecret() : totpService.generateSecret();
        List<String> backupCodes = StringUtils.hasText(account.getMfaRecoveryCodesCsv())
            ? List.of(account.getMfaRecoveryCodesCsv().split(","))
            : totpService.generateBackupCodes();

        account.setMfaSecret(secret);
        account.setMfaRecoveryCodesCsv(String.join(",", backupCodes));
        account.setMfaEnabled(false);
        userRepository.save(account);
        auditService.recordSystem("MFA_SETUP", "user", account.getId().toString(), "MFA setup initialized", account.getEmail());

        return new MfaSetupResponse(secret, totpService.buildOtpAuthUrl(ISSUER, account.getEmail(), secret), backupCodes);
    }

    public UserSummaryResponse verifyMfa(UserPrincipal principal, MfaVerifyRequest request) {
        UserAccount account = loadAccount(principal.getId());
        if (!StringUtils.hasText(account.getMfaSecret())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "MFA has not been initialized");
        }

        if (!verifyMfaCode(account, request.code())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid MFA code");
        }

        account.setMfaEnabled(true);
        userRepository.save(account);
        auditService.recordSystem("MFA_VERIFIED", "user", account.getId().toString(), "MFA verified", account.getEmail());
        return toSummary(account);
    }

    public UserSummaryResponse me(UserPrincipal principal) {
        return toSummary(loadAccount(principal.getId()));
    }

    public UserAccount loadAccount(UUID id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private AuthResponse issueSession(UserAccount account, String ipAddress, String userAgent) {
        String accessToken = jwtService.generateAccessToken(account);
        String refreshToken = createRefreshToken();
        RefreshTokenSession session = new RefreshTokenSession();
        session.setUserId(account.getId());
        session.setTokenHash(hashToken(refreshToken));
        session.setExpiresAt(Instant.now().plusSeconds(jwtService.getRefreshTokenDays() * 24 * 60 * 60));
        session.setLastUsedAt(Instant.now());
        session.setIpAddress(ipAddress);
        session.setUserAgent(userAgent);
        refreshTokenSessionRepository.save(session);
        return new AuthResponse(
            accessToken,
            refreshToken,
            "Bearer",
            jwtService.getAccessTokenMinutes() * 60,
            jwtService.getRefreshTokenDays() * 24 * 60 * 60,
            toSummary(account),
            account.isMfaEnabled()
        );
    }

    private RefreshTokenSession loadActiveSession(String refreshToken) {
        String tokenHash = hashToken(refreshToken);
        RefreshTokenSession session = refreshTokenSessionRepository.findByTokenHash(tokenHash)
            .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));
        if (session.getRevokedAt() != null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Refresh token has been revoked");
        }
        if (session.getExpiresAt().isBefore(Instant.now())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Refresh token has expired");
        }
        return session;
    }

    private String createRefreshToken() {
        byte[] bytes = new byte[48];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String refreshToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(refreshToken.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(hash);
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to process refresh token");
        }
    }

    private boolean verifyMfaCode(UserAccount account, String code) {
        if (totpService.verifyCode(account.getMfaSecret(), code)) {
            return true;
        }

        if (!StringUtils.hasText(account.getMfaRecoveryCodesCsv())) {
            return false;
        }

        Set<String> recoveryCodes = new LinkedHashSet<>(List.of(account.getMfaRecoveryCodesCsv().split(",")));
        boolean matched = recoveryCodes.remove(code.trim().toUpperCase());
        if (matched) {
            account.setMfaRecoveryCodesCsv(String.join(",", recoveryCodes));
            userRepository.save(account);
        }
        return matched;
    }

    private UserSummaryResponse toSummary(UserAccount account) {
        return new UserSummaryResponse(
            account.getId(),
            account.getEmail(),
            account.getFullName(),
            account.getCompanyName(),
            account.getRole(),
            account.isMfaEnabled(),
            kycService.getStatus(account.getId())
        );
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
}
