package com.apimarketplace.controller;

import com.apimarketplace.dto.keys.ApiKeyResponse;
import com.apimarketplace.dto.keys.CreateApiKeyRequest;
import com.apimarketplace.dto.keys.RevokeApiKeyResponse;
import com.apimarketplace.security.UserPrincipal;
import com.apimarketplace.service.ApiKeyService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/keys")
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    public ApiKeyController(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    @GetMapping
    public List<ApiKeyResponse> list(@AuthenticationPrincipal UserPrincipal principal) {
        return apiKeyService.listForUser(principal);
    }

    @PostMapping
    public ApiKeyResponse create(
        @AuthenticationPrincipal UserPrincipal principal,
        @Valid @RequestBody CreateApiKeyRequest request
    ) {
        return apiKeyService.create(principal, request);
    }

    @DeleteMapping("/{id}")
    public RevokeApiKeyResponse revoke(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable UUID id
    ) {
        ApiKeyResponse response = apiKeyService.revoke(principal, id);
        return new RevokeApiKeyResponse(response.id(), response.active());
    }
}
