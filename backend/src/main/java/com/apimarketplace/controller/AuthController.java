package com.apimarketplace.controller;

import com.apimarketplace.dto.auth.AuthResponse;
import com.apimarketplace.dto.auth.LoginRequest;
import com.apimarketplace.dto.auth.LogoutRequest;
import com.apimarketplace.dto.auth.MfaSetupResponse;
import com.apimarketplace.dto.auth.MfaVerifyRequest;
import com.apimarketplace.dto.auth.RefreshTokenRequest;
import com.apimarketplace.dto.auth.RegisterRequest;
import com.apimarketplace.dto.auth.UserSummaryResponse;
import com.apimarketplace.security.UserPrincipal;
import com.apimarketplace.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "Authentication, MFA, refresh-token rotation, and session logout")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register user", description = "Creates an account and returns access plus refresh tokens.")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
        return authService.register(request, httpRequest.getRemoteAddr(), httpRequest.getHeader("User-Agent"));
    }

    @PostMapping("/login")
    @Operation(summary = "Login user", description = "Authenticates the user and returns access plus refresh tokens.")
    public AuthResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        return authService.login(request, httpRequest.getRemoteAddr(), httpRequest.getHeader("User-Agent"));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh session", description = "Rotates the refresh token and returns a new access token pair.")
    public AuthResponse refresh(@Valid @RequestBody RefreshTokenRequest request, HttpServletRequest httpRequest) {
        return authService.refresh(request, httpRequest.getRemoteAddr(), httpRequest.getHeader("User-Agent"));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout user", description = "Revokes the submitted refresh token for the authenticated user.")
    public void logout(
        @AuthenticationPrincipal UserPrincipal principal,
        @Valid @RequestBody LogoutRequest request
    ) {
        authService.logout(principal, request);
    }

    @PostMapping("/mfa/setup")
    @Operation(summary = "Initialize MFA", description = "Generates a TOTP secret, QR URL, and recovery codes.")
    public MfaSetupResponse setupMfa(@AuthenticationPrincipal UserPrincipal principal) {
        return authService.setupMfa(principal);
    }

    @PostMapping("/mfa/verify")
    @Operation(summary = "Verify MFA", description = "Enables MFA after validating the submitted TOTP or backup code.")
    public UserSummaryResponse verifyMfa(
        @AuthenticationPrincipal UserPrincipal principal,
        @Valid @RequestBody MfaVerifyRequest request
    ) {
        return authService.verifyMfa(principal, request);
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user", description = "Returns the authenticated user's profile summary.")
    public UserSummaryResponse me(@AuthenticationPrincipal UserPrincipal principal) {
        return authService.me(principal);
    }
}
