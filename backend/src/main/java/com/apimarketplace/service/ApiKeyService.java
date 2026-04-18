package com.apimarketplace.service;

import com.apimarketplace.dto.keys.ApiKeyResponse;
import com.apimarketplace.dto.keys.CreateApiKeyRequest;
import com.apimarketplace.dto.keys.RevokeApiKeyResponse;
import com.apimarketplace.entity.ApiKey;
import com.apimarketplace.entity.ApiProduct;
import com.apimarketplace.entity.Subscription;
import com.apimarketplace.entity.enums.SubscriptionStatus;
import com.apimarketplace.entity.enums.UserRole;
import com.apimarketplace.exception.ApiException;
import com.apimarketplace.repository.ApiKeyRepository;
import com.apimarketplace.repository.ApiProductRepository;
import com.apimarketplace.repository.SubscriptionRepository;
import com.apimarketplace.security.UserPrincipal;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.cache.annotation.CacheEvict;

@Service
public class ApiKeyService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int LOOKUP_TOKEN_LENGTH = 12;
    private static final int SECRET_TOKEN_LENGTH = 24;

    private final ApiKeyRepository apiKeyRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final ApiProductRepository apiProductRepository;
    private final ApiKeyLookupCacheService apiKeyLookupCacheService;
    private final KycService kycService;
    private final AuditService auditService;
    private final NotificationService notificationService;
    private final PasswordEncoder passwordEncoder;

    public ApiKeyService(
        ApiKeyRepository apiKeyRepository,
        SubscriptionRepository subscriptionRepository,
        ApiProductRepository apiProductRepository,
        ApiKeyLookupCacheService apiKeyLookupCacheService,
        KycService kycService,
        AuditService auditService,
        NotificationService notificationService,
        PasswordEncoder passwordEncoder
    ) {
        this.apiKeyRepository = apiKeyRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.apiProductRepository = apiProductRepository;
        this.apiKeyLookupCacheService = apiKeyLookupCacheService;
        this.kycService = kycService;
        this.auditService = auditService;
        this.notificationService = notificationService;
        this.passwordEncoder = passwordEncoder;
    }

    public List<ApiKeyResponse> listForUser(UserPrincipal principal) {
        return apiKeyRepository.findByOwnerIdOrderByCreatedAtDesc(principal.getId())
            .stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    @CacheEvict(cacheNames = "apiKeyPrefixCandidates", allEntries = true)
    public ApiKeyResponse create(UserPrincipal principal, CreateApiKeyRequest request) {
        kycService.requireApproved(principal, "create API keys");

        Subscription subscription = subscriptionRepository.findByIdAndConsumerId(request.subscriptionId(), principal.getId())
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Subscription not found"));

        if (subscription.getStatus() != SubscriptionStatus.ACTIVE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Subscription is not active");
        }

        ApiProduct product = apiProductRepository.findById(subscription.getApiProductId())
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "API product not found"));

        String plainKey = generatePlainKey();
        ApiKey apiKey = new ApiKey();
        apiKey.setSubscriptionId(subscription.getId());
        apiKey.setOwnerId(principal.getId());
        apiKey.setApiProductId(product.getId());
        apiKey.setKeyPrefix(extractLookupToken(plainKey));
        apiKey.setKeyHash(passwordEncoder.encode(plainKey));
        apiKey.setLabel(StringUtils.hasText(request.label()) ? request.label().trim() : product.getName() + " key");
        apiKey.setScopesCsv(normalizeScopes(request.scopes()));
        apiKey.setExpiresAt(resolveExpiry(request.expiresInDays()));
        apiKey.setActive(true);

        ApiKey saved = apiKeyRepository.save(apiKey);
        auditService.record(principal, "API_KEY_CREATED", "api_key", saved.getId().toString(), "SUCCESS", "API key created", saved.getSubscriptionId().toString());
        notificationService.notifyUserEmail(
            principal.getId(),
            "API key created",
            "A new API key has been created for " + product.getName() + ".",
            "API_KEY_CREATED"
        );
        return toResponse(saved, plainKey);
    }

    @CacheEvict(cacheNames = "apiKeyPrefixCandidates", allEntries = true)
    public ApiKeyResponse revoke(UserPrincipal principal, UUID keyId) {
        ApiKey apiKey = loadOwnedKey(principal, keyId);
        apiKey.setActive(false);
        ApiKey saved = apiKeyRepository.save(apiKey);
        auditService.record(principal, "API_KEY_REVOKED", "api_key", saved.getId().toString(), "SUCCESS", "API key revoked", saved.getSubscriptionId().toString());
        notificationService.notifyUserEmail(
            principal.getId(),
            "API key revoked",
            "An API key has been revoked.",
            "API_KEY_REVOKED"
        );
        return toResponse(saved);
    }

    public ApiKey validateRawKey(String rawKey) {
        if (!StringUtils.hasText(rawKey) || !rawKey.startsWith("apim_") || rawKey.length() <= 16) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid API key");
        }

        String lookupToken = extractLookupToken(rawKey);
        List<ApiKey> candidates = apiKeyLookupCacheService.findActiveKeysByPrefix(lookupToken);
        for (ApiKey candidate : candidates) {
            if (candidate.getExpiresAt() != null && candidate.getExpiresAt().isBefore(Instant.now())) {
                continue;
            }
            if (passwordEncoder.matches(rawKey, candidate.getKeyHash())) {
                candidate.setLastUsedAt(Instant.now());
                apiKeyRepository.save(candidate);
                return candidate;
            }
        }

        throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid API key");
    }

    public ApiKey loadById(UUID keyId) {
        return apiKeyRepository.findById(keyId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "API key not found"));
    }

    private ApiKey loadOwnedKey(UserPrincipal principal, UUID keyId) {
        ApiKey apiKey = loadById(keyId);
        if (!apiKey.getOwnerId().equals(principal.getId()) && principal.getRole() != UserRole.ADMIN) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You do not have access to this API key");
        }
        return apiKey;
    }

    private String generatePlainKey() {
        String lookupToken = randomToken(LOOKUP_TOKEN_LENGTH);
        String secretToken = randomToken(SECRET_TOKEN_LENGTH);
        return "apim_" + lookupToken + secretToken;
    }

    private String extractLookupToken(String rawKey) {
        String trimmed = rawKey.startsWith("apim_") ? rawKey.substring(5) : rawKey;
        return trimmed.substring(0, LOOKUP_TOKEN_LENGTH);
    }

    private String randomToken(int length) {
        String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            builder.append(alphabet.charAt(RANDOM.nextInt(alphabet.length())));
        }
        return builder.toString();
    }

    private String normalizeScopes(List<String> scopes) {
        List<String> sanitized = new ArrayList<>();
        if (scopes != null) {
            for (String scope : scopes) {
                if (StringUtils.hasText(scope)) {
                    sanitized.add(scope.trim().toLowerCase(Locale.ROOT));
                }
            }
        }
        if (sanitized.isEmpty()) {
            sanitized.add("read");
            sanitized.add("write");
        }
        return String.join(",", sanitized);
    }

    private Instant resolveExpiry(Integer expiresInDays) {
        int days = expiresInDays == null || expiresInDays <= 0 ? 365 : expiresInDays;
        return Instant.now().plus(days, ChronoUnit.DAYS);
    }

    private ApiKeyResponse toResponse(ApiKey apiKey) {
        return toResponse(apiKey, null);
    }

    private ApiKeyResponse toResponse(ApiKey apiKey, String plainKey) {
        return new ApiKeyResponse(
            apiKey.getId(),
            apiKey.getSubscriptionId(),
            apiKey.getApiProductId(),
            apiKey.getLabel(),
            List.of(apiKey.getScopesCsv().split(",")),
            apiKey.isActive(),
            apiKey.getExpiresAt(),
            apiKey.getLastUsedAt(),
            apiKey.getCreatedAt(),
            apiKey.getUpdatedAt(),
            plainKey
        );
    }
}
