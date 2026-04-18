package com.apimarketplace.service;

import com.apimarketplace.dto.subscription.SubscribeRequest;
import com.apimarketplace.dto.subscription.SubscriptionResponse;
import com.apimarketplace.entity.ApiProduct;
import com.apimarketplace.entity.Subscription;
import com.apimarketplace.entity.enums.SubscriptionStatus;
import com.apimarketplace.exception.ApiException;
import com.apimarketplace.repository.ApiProductRepository;
import com.apimarketplace.repository.SubscriptionRepository;
import com.apimarketplace.security.UserPrincipal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class SubscriptionService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Kolkata");

    private final SubscriptionRepository subscriptionRepository;
    private final ApiProductRepository apiProductRepository;
    private final BillingService billingService;
    private final KycService kycService;
    private final AuditService auditService;
    private final NotificationService notificationService;

    public SubscriptionService(
        SubscriptionRepository subscriptionRepository,
        ApiProductRepository apiProductRepository,
        BillingService billingService,
        KycService kycService,
        AuditService auditService,
        NotificationService notificationService
    ) {
        this.subscriptionRepository = subscriptionRepository;
        this.apiProductRepository = apiProductRepository;
        this.billingService = billingService;
        this.kycService = kycService;
        this.auditService = auditService;
        this.notificationService = notificationService;
    }

    public List<SubscriptionResponse> listForUser(UserPrincipal principal) {
        return subscriptionRepository.findByConsumerIdOrderByCreatedAtDesc(principal.getId())
            .stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    @CacheEvict(cacheNames = "providerAnalytics", allEntries = true)
    public SubscriptionResponse subscribe(UserPrincipal principal, SubscribeRequest request) {
        kycService.requireApproved(principal, "subscribe to APIs");

        ApiProduct product = apiProductRepository.findById(request.apiProductId())
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "API product not found"));

        if (!product.isActive()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "API product is not active");
        }

        subscriptionRepository.findByConsumerIdAndApiProductId(principal.getId(), request.apiProductId())
            .filter(subscription -> subscription.getStatus() == SubscriptionStatus.ACTIVE)
            .ifPresent(subscription -> {
                throw new ApiException(HttpStatus.CONFLICT, "You are already subscribed to this API");
            });

        Subscription subscription = new Subscription();
        subscription.setConsumerId(principal.getId());
        subscription.setApiProductId(product.getId());
        subscription.setPlanName(request.planName().trim());
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setMonthlyPrice(product.getBasePrice());
        subscription.setMonthlyRequestLimit(calculateMonthlyRequestLimit(product));
        subscription.setStartedAt(Instant.now());

        Subscription saved = subscriptionRepository.save(subscription);
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        billingService.createInvoiceForSubscription(saved, today.withDayOfMonth(1), today.withDayOfMonth(today.lengthOfMonth()));
        auditService.record(principal, "SUBSCRIPTION_CREATED", "subscription", saved.getId().toString(), "SUCCESS", "Subscription created", saved.getApiProductId().toString());
        notificationService.notifyUserEmail(
            principal.getId(),
            "Subscription activated",
            "Your subscription to " + product.getName() + " is active.",
            "SUBSCRIPTION_CREATED"
        );
        return toResponse(saved);
    }

    @CacheEvict(cacheNames = "providerAnalytics", allEntries = true)
    public SubscriptionResponse cancel(UserPrincipal principal, UUID subscriptionId) {
        Subscription subscription = subscriptionRepository.findByIdAndConsumerId(subscriptionId, principal.getId())
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Subscription not found"));
        subscription.setStatus(SubscriptionStatus.CANCELLED);
        subscription.setCancelledAt(Instant.now());
        Subscription saved = subscriptionRepository.save(subscription);
        auditService.record(principal, "SUBSCRIPTION_CANCELLED", "subscription", saved.getId().toString(), "SUCCESS", "Subscription cancelled", saved.getApiProductId().toString());
        notificationService.notifyUserEmail(
            principal.getId(),
            "Subscription cancelled",
            "Your subscription has been cancelled.",
            "SUBSCRIPTION_CANCELLED"
        );
        return toResponse(saved);
    }

    private int calculateMonthlyRequestLimit(ApiProduct product) {
        long estimate = Math.max(1L, product.getRequestsPerMinute()) * 43_200L;
        return (int) Math.min(Integer.MAX_VALUE, estimate);
    }

    private SubscriptionResponse toResponse(Subscription subscription) {
        ApiProduct apiProduct = apiProductRepository.findById(subscription.getApiProductId()).orElse(null);
        return new SubscriptionResponse(
            subscription.getId(),
            subscription.getConsumerId(),
            subscription.getApiProductId(),
            apiProduct == null ? "Unknown API" : apiProduct.getName(),
            apiProduct == null ? "Unknown" : apiProduct.getCategory(),
            subscription.getPlanName(),
            subscription.getStatus(),
            subscription.getMonthlyPrice(),
            subscription.getMonthlyRequestLimit(),
            subscription.getStartedAt(),
            subscription.getCancelledAt(),
            subscription.getCreatedAt(),
            subscription.getUpdatedAt()
        );
    }
}
