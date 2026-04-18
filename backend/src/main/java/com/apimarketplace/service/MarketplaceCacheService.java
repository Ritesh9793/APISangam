package com.apimarketplace.service;

import com.apimarketplace.dto.api.ApiProductResponse;
import com.apimarketplace.entity.ApiKey;
import com.apimarketplace.entity.ApiProduct;
import com.apimarketplace.entity.UserAccount;
import com.apimarketplace.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class MarketplaceCacheService {

    private final StringRedisTemplate redisTemplate;
    private final UserRepository userRepository;
    private final long apiCatalogTtlSeconds;
    private final long apiKeyValidationTtlSeconds;

    public MarketplaceCacheService(
        StringRedisTemplate redisTemplate,
        UserRepository userRepository,
        @Value("${app.cache.api-catalog-ttl-seconds:300}") long apiCatalogTtlSeconds,
        @Value("${app.cache.api-key-validation-ttl-seconds:300}") long apiKeyValidationTtlSeconds
    ) {
        this.redisTemplate = redisTemplate;
        this.userRepository = userRepository;
        this.apiCatalogTtlSeconds = apiCatalogTtlSeconds;
        this.apiKeyValidationTtlSeconds = apiKeyValidationTtlSeconds;
    }

    public List<ApiProductResponse> getCachedApiCatalog(String cacheKey, Supplier<List<ApiProductResponse>> loader) {
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (StringUtils.hasText(cached)) {
            return ApiProductCacheCodec.decodeList(cached);
        }

        List<ApiProductResponse> loaded = loader.get();
        redisTemplate.opsForValue().set(cacheKey, ApiProductCacheCodec.encodeList(loaded), Duration.ofSeconds(apiCatalogTtlSeconds));
        return loaded;
    }

    public Optional<ApiProductResponse> getCachedApiProduct(String cacheKey, Supplier<ApiProductResponse> loader) {
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (StringUtils.hasText(cached)) {
            return Optional.of(ApiProductCacheCodec.decodeOne(cached));
        }

        ApiProductResponse loaded = loader.get();
        redisTemplate.opsForValue().set(cacheKey, ApiProductCacheCodec.encodeOne(loaded), Duration.ofSeconds(apiCatalogTtlSeconds));
        return Optional.of(loaded);
    }

    public void evictApiCatalogCaches() {
        redisTemplate.delete(List.of(
            "cache:api-catalog:all",
            "cache:api-catalog:active"
        ));
    }

    public Optional<ApiKeyCacheRecord> getValidatedApiKey(String rawKey) {
        String cached = redisTemplate.opsForValue().get(apiKeyCacheKey(rawKey));
        if (!StringUtils.hasText(cached)) {
            return Optional.empty();
        }
        return Optional.of(ApiKeyCacheRecord.decode(cached));
    }

    public void cacheValidatedApiKey(String rawKey, ApiKey apiKey) {
        ApiKeyCacheRecord record = new ApiKeyCacheRecord(
            apiKey.getId(),
            apiKey.getSubscriptionId(),
            apiKey.getApiProductId(),
            apiKey.getOwnerId(),
            apiKey.getKeyPrefix(),
            apiKey.getScopesCsv(),
            apiKey.isActive(),
            apiKey.getExpiresAt()
        );
        redisTemplate.opsForValue().set(apiKeyCacheKey(rawKey), record.encode(), Duration.ofSeconds(apiKeyValidationTtlSeconds));
    }

    public void evictValidatedApiKey(String rawKey) {
        redisTemplate.delete(apiKeyCacheKey(rawKey));
    }

    public void evictValidatedApiKeyById(UUID apiKeyId) {
        String pattern = "cache:api-key:validated:*:" + apiKeyId;
        var keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    public String getProviderName(UUID providerId) {
        return userRepository.findById(providerId).map(UserAccount::getFullName).orElse("Unknown provider");
    }

    private String apiKeyCacheKey(String rawKey) {
        return "cache:api-key:validated:" + sha256Hex(rawKey);
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(hash);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to hash cache key", ex);
        }
    }

    private record ApiKeyCacheRecord(
        UUID apiKeyId,
        UUID subscriptionId,
        UUID apiProductId,
        UUID ownerId,
        String keyPrefix,
        String scopesCsv,
        boolean active,
        java.time.Instant expiresAt
    ) {
        private String encode() {
            return String.join("|",
                apiKeyId.toString(),
                subscriptionId.toString(),
                apiProductId.toString(),
                ownerId.toString(),
                keyPrefix,
                scopesCsv == null ? "" : scopesCsv,
                Boolean.toString(active),
                expiresAt == null ? "" : expiresAt.toString()
            );
        }

        private static ApiKeyCacheRecord decode(String encoded) {
            String[] parts = encoded.split("\\|", -1);
            return new ApiKeyCacheRecord(
                UUID.fromString(parts[0]),
                UUID.fromString(parts[1]),
                UUID.fromString(parts[2]),
                UUID.fromString(parts[3]),
                parts[4],
                parts[5],
                Boolean.parseBoolean(parts[6]),
                parts[7].isBlank() ? null : java.time.Instant.parse(parts[7])
            );
        }
    }

    private static final class ApiProductCacheCodec {
        private static String encodeOne(ApiProductResponse product) {
            return String.join("|",
                product.id().toString(),
                product.providerId().toString(),
                safe(product.providerName()),
                safe(product.name()),
                safe(product.slug()),
                safe(product.description()),
                safe(product.category()),
                safe(product.documentationUrl()),
                safe(product.openApiSpecUrl()),
                product.basePrice().toPlainString(),
                String.valueOf(product.requestsPerMinute()),
                Boolean.toString(product.active()),
                safe(product.region()),
                product.createdAt().toString(),
                product.updatedAt().toString()
            );
        }

        private static ApiProductResponse decodeOne(String encoded) {
            String[] parts = encoded.split("\\|", -1);
            return new ApiProductResponse(
                UUID.fromString(parts[0]),
                UUID.fromString(parts[1]),
                unsaf(parts[2]),
                unsaf(parts[3]),
                unsaf(parts[4]),
                unsaf(parts[5]),
                unsaf(parts[6]),
                emptyToNull(parts[7]),
                emptyToNull(parts[8]),
                new java.math.BigDecimal(parts[9]),
                Integer.valueOf(parts[10]),
                Boolean.parseBoolean(parts[11]),
                emptyToNull(parts[12]),
                java.time.Instant.parse(parts[13]),
                java.time.Instant.parse(parts[14])
            );
        }

        private static String encodeList(List<ApiProductResponse> products) {
            return products.stream().map(ApiProductCacheCodec::encodeOne).collect(java.util.stream.Collectors.joining("~"));
        }

        private static List<ApiProductResponse> decodeList(String encoded) {
            if (!StringUtils.hasText(encoded)) {
                return List.of();
            }
            return java.util.Arrays.stream(encoded.split("~", -1))
                .filter(StringUtils::hasText)
                .map(ApiProductCacheCodec::decodeOne)
                .collect(java.util.stream.Collectors.toList());
        }

        private static String safe(String value) {
            return value == null ? "" : value.replace("|", "/");
        }

        private static String unsaf(String value) {
            return value;
        }

        private static String emptyToNull(String value) {
            return StringUtils.hasText(value) ? value : null;
        }
    }
}
