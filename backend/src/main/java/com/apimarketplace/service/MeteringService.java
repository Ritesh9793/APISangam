package com.apimarketplace.service;

import com.apimarketplace.dto.metering.ApiCallLogRequest;
import com.apimarketplace.dto.metering.ApiCallLogResponse;
import com.apimarketplace.entity.ApiCallLog;
import com.apimarketplace.entity.ApiKey;
import com.apimarketplace.entity.ApiProduct;
import com.apimarketplace.entity.Subscription;
import com.apimarketplace.entity.enums.SubscriptionStatus;
import com.apimarketplace.exception.ApiException;
import com.apimarketplace.repository.ApiCallLogRepository;
import com.apimarketplace.repository.ApiProductRepository;
import com.apimarketplace.repository.SubscriptionRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class MeteringService {

    private final ApiKeyService apiKeyService;
    private final RateLimitService rateLimitService;
    private final ApiCallLogRepository apiCallLogRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final ApiProductRepository apiProductRepository;
    private final BillingService billingService;

    public MeteringService(
        ApiKeyService apiKeyService,
        RateLimitService rateLimitService,
        ApiCallLogRepository apiCallLogRepository,
        SubscriptionRepository subscriptionRepository,
        ApiProductRepository apiProductRepository,
        BillingService billingService
    ) {
        this.apiKeyService = apiKeyService;
        this.rateLimitService = rateLimitService;
        this.apiCallLogRepository = apiCallLogRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.apiProductRepository = apiProductRepository;
        this.billingService = billingService;
    }

    @CacheEvict(cacheNames = "providerAnalytics", allEntries = true)
    public ApiCallLogResponse record(ApiCallLogRequest request) {
        ApiKey apiKey = apiKeyService.validateRawKey(request.apiKey());
        Subscription subscription = subscriptionRepository.findById(apiKey.getSubscriptionId())
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Subscription not found"));

        if (subscription.getStatus() != SubscriptionStatus.ACTIVE) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Subscription is not active");
        }

        ApiProduct product = apiProductRepository.findById(apiKey.getApiProductId())
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "API product not found"));

        RateLimitService.RateLimitDecision decision = rateLimitService.allow(apiKey.getId().toString(), product.getRequestsPerMinute());
        if (!decision.allowed()) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded");
        }

        BigDecimal requestCost = request.requestCost() == null
            ? BigDecimal.ZERO
            : request.requestCost().setScale(2, RoundingMode.HALF_UP);

        ApiCallLog log = new ApiCallLog();
        log.setApiKeyId(apiKey.getId());
        log.setSubscriptionId(subscription.getId());
        log.setApiProductId(product.getId());
        log.setRequestPath(request.requestPath());
        log.setHttpMethod(request.httpMethod());
        log.setStatusCode(request.statusCode());
        log.setLatencyMs(request.latencyMs());
        log.setRequestCost(requestCost);
        log.setIpAddress(StringUtils.hasText(request.ipAddress()) ? request.ipAddress().trim() : null);
        log.setUserAgent(StringUtils.hasText(request.userAgent()) ? request.userAgent().trim() : null);
        log.setOccurredAt(Instant.now());
        ApiCallLog saved = apiCallLogRepository.save(log);

        billingService.recordUsage(subscription.getId(), 1L, requestCost);

        return new ApiCallLogResponse(
            saved.getId(),
            true,
            saved.getStatusCode(),
            decision.remainingRequests(),
            subscription.getId(),
            product.getId(),
            saved.getRequestCost(),
            saved.getOccurredAt()
        );
    }
}
