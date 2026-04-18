package com.apimarketplace.service;

import com.apimarketplace.dto.analytics.ApiPerformanceSummary;
import com.apimarketplace.dto.analytics.ProviderAnalyticsResponse;
import com.apimarketplace.entity.ApiProduct;
import com.apimarketplace.entity.Invoice;
import com.apimarketplace.entity.Subscription;
import com.apimarketplace.entity.UsageRecord;
import com.apimarketplace.entity.enums.InvoiceStatus;
import com.apimarketplace.entity.enums.UserRole;
import com.apimarketplace.exception.ApiException;
import com.apimarketplace.repository.ApiProductRepository;
import com.apimarketplace.repository.InvoiceRepository;
import com.apimarketplace.repository.SubscriptionRepository;
import com.apimarketplace.repository.UsageRecordRepository;
import com.apimarketplace.security.UserPrincipal;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class ProviderAnalyticsService {

    private final ApiProductRepository apiProductRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final InvoiceRepository invoiceRepository;
    private final UsageRecordRepository usageRecordRepository;

    public ProviderAnalyticsService(
        ApiProductRepository apiProductRepository,
        SubscriptionRepository subscriptionRepository,
        InvoiceRepository invoiceRepository,
        UsageRecordRepository usageRecordRepository
    ) {
        this.apiProductRepository = apiProductRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.invoiceRepository = invoiceRepository;
        this.usageRecordRepository = usageRecordRepository;
    }

    @Cacheable(cacheNames = "providerAnalytics", key = "#principal.id")
    public ProviderAnalyticsResponse overview(UserPrincipal principal) {
        if (principal.getRole() != UserRole.PROVIDER && principal.getRole() != UserRole.ADMIN) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only providers can view analytics");
        }

        List<ApiProduct> products = principal.getRole() == UserRole.ADMIN
            ? apiProductRepository.findAll()
            : apiProductRepository.findByProviderId(principal.getId());

        List<UUID> productIds = products.stream().map(ApiProduct::getId).toList();
        List<Subscription> subscriptions = productIds.isEmpty()
            ? List.of()
            : subscriptionRepository.findByApiProductIdIn(productIds);

        List<UUID> subscriptionIds = subscriptions.stream().map(Subscription::getId).toList();
        List<Invoice> invoices = subscriptionIds.isEmpty()
            ? List.of()
            : invoiceRepository.findBySubscriptionIdIn(subscriptionIds);
        List<UsageRecord> usageRecords = subscriptionIds.isEmpty()
            ? List.of()
            : usageRecordRepository.findBySubscriptionIdIn(subscriptionIds);

        long activeApis = products.stream().filter(ApiProduct::isActive).count();
        long activeSubscriptions = subscriptions.stream().filter(sub -> sub.getStatus() == com.apimarketplace.entity.enums.SubscriptionStatus.ACTIVE).count();
        long totalCalls = usageRecords.stream().mapToLong(UsageRecord::getCallCount).sum();
        BigDecimal totalRevenue = invoices.stream()
            .filter(invoice -> invoice.getStatus() == InvoiceStatus.PAID)
            .map(Invoice::getTotalAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal pendingRevenue = invoices.stream()
            .filter(invoice -> invoice.getStatus() == InvoiceStatus.PENDING)
            .map(Invoice::getTotalAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<UUID, Long> subscriptionCounts = new HashMap<>();
        for (Subscription subscription : subscriptions) {
            subscriptionCounts.merge(subscription.getApiProductId(), 1L, Long::sum);
        }

        Map<UUID, Long> callCounts = new HashMap<>();
        for (UsageRecord usageRecord : usageRecords) {
            subscriptionRepository.findById(usageRecord.getSubscriptionId())
                .ifPresent(subscription -> callCounts.merge(subscription.getApiProductId(), usageRecord.getCallCount(), Long::sum));
        }

        Map<UUID, BigDecimal> revenueByProduct = new HashMap<>();
        for (Invoice invoice : invoices) {
            if (invoice.getStatus() != InvoiceStatus.PAID) {
                continue;
            }
            subscriptionRepository.findById(invoice.getSubscriptionId()).ifPresent(subscription ->
                revenueByProduct.merge(subscription.getApiProductId(), invoice.getTotalAmount(), BigDecimal::add)
            );
        }

        List<ApiPerformanceSummary> performance = new ArrayList<>();
        for (ApiProduct product : products) {
            performance.add(new ApiPerformanceSummary(
                product.getId(),
                product.getName(),
                product.getSlug(),
                subscriptionCounts.getOrDefault(product.getId(), 0L),
                callCounts.getOrDefault(product.getId(), 0L),
                revenueByProduct.getOrDefault(product.getId(), BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP)
            ));
        }

        performance.sort(Comparator.comparing(ApiPerformanceSummary::callCount).reversed());

        return new ProviderAnalyticsResponse(
            products.size(),
            (int) activeApis,
            subscriptions.size(),
            (int) activeSubscriptions,
            totalCalls,
            totalRevenue.setScale(2, RoundingMode.HALF_UP),
            pendingRevenue.setScale(2, RoundingMode.HALF_UP),
            performance
        );
    }
}
