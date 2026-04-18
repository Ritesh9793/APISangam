package com.apimarketplace.service;

import com.apimarketplace.dto.billing.InvoiceResponse;
import com.apimarketplace.dto.billing.UsageResponse;
import com.apimarketplace.entity.ApiProduct;
import com.apimarketplace.entity.Invoice;
import com.apimarketplace.entity.Subscription;
import com.apimarketplace.entity.UsageRecord;
import com.apimarketplace.entity.enums.InvoiceStatus;
import com.apimarketplace.entity.enums.SubscriptionStatus;
import com.apimarketplace.exception.ApiException;
import com.apimarketplace.repository.ApiProductRepository;
import com.apimarketplace.repository.InvoiceRepository;
import com.apimarketplace.repository.SubscriptionRepository;
import com.apimarketplace.repository.UsageRecordRepository;
import com.apimarketplace.security.UserPrincipal;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BillingService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Kolkata");

    private final InvoiceRepository invoiceRepository;
    private final UsageRecordRepository usageRecordRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final ApiProductRepository apiProductRepository;
    private final AuditService auditService;
    private final NotificationService notificationService;
    private final BigDecimal gstRate;

    public BillingService(
        InvoiceRepository invoiceRepository,
        UsageRecordRepository usageRecordRepository,
        SubscriptionRepository subscriptionRepository,
        ApiProductRepository apiProductRepository,
        AuditService auditService,
        NotificationService notificationService,
        @Value("${app.business.gst-rate:0.18}") BigDecimal gstRate
    ) {
        this.invoiceRepository = invoiceRepository;
        this.usageRecordRepository = usageRecordRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.apiProductRepository = apiProductRepository;
        this.auditService = auditService;
        this.notificationService = notificationService;
        this.gstRate = gstRate;
    }

    public List<InvoiceResponse> listInvoicesForUser(UserPrincipal principal) {
        return subscriptionRepository.findByConsumerIdOrderByCreatedAtDesc(principal.getId())
            .stream()
            .flatMap(subscription -> invoiceRepository.findBySubscriptionIdOrderByBillingPeriodEndDesc(subscription.getId()).stream())
            .sorted(Comparator.comparing(Invoice::getCreatedAt).reversed())
            .map(this::toInvoiceResponse)
            .collect(Collectors.toList());
    }

    public List<UsageResponse> listUsageForUser(UserPrincipal principal) {
        return subscriptionRepository.findByConsumerIdOrderByCreatedAtDesc(principal.getId())
            .stream()
            .flatMap(subscription -> usageRecordRepository.findBySubscriptionIdOrderByUsageDateDesc(subscription.getId()).stream())
            .sorted(Comparator.comparing(UsageRecord::getUsageDate).reversed())
            .map(this::toUsageResponse)
            .collect(Collectors.toList());
    }

    public Invoice createInvoiceForSubscription(Subscription subscription, LocalDate periodStart, LocalDate periodEnd) {
        if (invoiceRepository.existsBySubscriptionIdAndBillingPeriodStartAndBillingPeriodEnd(
            subscription.getId(),
            periodStart,
            periodEnd
        )) {
            return invoiceRepository.findBySubscriptionIdAndBillingPeriodStartAndBillingPeriodEnd(
                subscription.getId(),
                periodStart,
                periodEnd
            ).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Invoice already exists"));
        }

        BigDecimal baseAmount = normalizeMoney(subscription.getMonthlyPrice());
        BigDecimal taxAmount = baseAmount.multiply(gstRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = baseAmount.add(taxAmount).setScale(2, RoundingMode.HALF_UP);

        Invoice invoice = new Invoice();
        invoice.setSubscriptionId(subscription.getId());
        invoice.setInvoiceNumber(generateInvoiceNumber(periodEnd));
        invoice.setStatus(InvoiceStatus.PENDING);
        invoice.setBillingPeriodStart(periodStart);
        invoice.setBillingPeriodEnd(periodEnd);
        invoice.setBaseAmount(baseAmount);
        invoice.setTaxAmount(taxAmount);
        invoice.setTotalAmount(totalAmount);
        invoice.setDueAt(Instant.now().plusSeconds(15L * 24L * 60L * 60L));
        invoice.setPaymentProvider("UNPAID");
        Invoice saved = invoiceRepository.save(invoice);
        auditService.recordSystem("INVOICE_CREATED", "invoice", saved.getId().toString(), "Invoice created", saved.getInvoiceNumber());
        subscriptionRepository.findById(subscription.getId())
            .ifPresent(savedSubscription -> apiProductRepository.findById(savedSubscription.getApiProductId())
                .ifPresent(apiProduct -> notificationService.notifyUserEmail(
                    subscription.getConsumerId(),
                    "Invoice generated for " + apiProduct.getName(),
                    "Your invoice " + saved.getInvoiceNumber() + " for " + saved.getTotalAmount() + " INR is ready.",
                    "INVOICE_CREATED"
                )));
        return saved;
    }

    @Transactional
    public UsageRecord recordUsage(UUID subscriptionId, long callCount, BigDecimal cost) {
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        UsageRecord record = usageRecordRepository.findBySubscriptionIdAndUsageDate(subscriptionId, today)
            .orElseGet(() -> {
                UsageRecord fresh = new UsageRecord();
                fresh.setSubscriptionId(subscriptionId);
                fresh.setUsageDate(today);
                fresh.setCallCount(0L);
                fresh.setTotalCost(BigDecimal.ZERO);
                return fresh;
            });
        record.setCallCount(record.getCallCount() + callCount);
        record.setTotalCost(normalizeMoney(record.getTotalCost().add(cost)));
        return usageRecordRepository.save(record);
    }

    @Scheduled(cron = "0 0 1 1 * *")
    public void generateMonthlyInvoices() {
        LocalDate current = LocalDate.now(BUSINESS_ZONE);
        LocalDate periodStart = current.withDayOfMonth(1);
        LocalDate periodEnd = current.withDayOfMonth(current.lengthOfMonth());

        subscriptionRepository.findByStatus(SubscriptionStatus.ACTIVE).forEach(subscription ->
            createInvoiceForSubscription(subscription, periodStart, periodEnd)
        );
    }

    public InvoiceResponse markInvoicePaid(UUID invoiceId, String paymentProvider, String paymentReference) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Invoice not found"));
        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setPaidAt(Instant.now());
        invoice.setPaymentProvider(paymentProvider);
        invoice.setPaymentReference(paymentReference);
        Invoice saved = invoiceRepository.save(invoice);
        auditService.recordSystem("INVOICE_PAID", "invoice", saved.getId().toString(), "Invoice paid", paymentProvider + ":" + paymentReference);
        subscriptionRepository.findById(saved.getSubscriptionId())
            .ifPresent(subscription -> notificationService.notifyUserEmail(
                subscription.getConsumerId(),
                "Payment received for " + saved.getInvoiceNumber(),
                "Your payment for invoice " + saved.getInvoiceNumber() + " was recorded successfully.",
                "INVOICE_PAID"
            ));
        return toInvoiceResponse(saved);
    }

    public InvoiceResponse getInvoice(UUID invoiceId, UserPrincipal principal) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Invoice not found"));
        ensureInvoiceBelongsToUser(invoice, principal.getId());
        return toInvoiceResponse(invoice);
    }

    private void ensureInvoiceBelongsToUser(Invoice invoice, UUID userId) {
        Subscription subscription = subscriptionRepository.findById(invoice.getSubscriptionId())
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Subscription not found"));
        if (!subscription.getConsumerId().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You do not have access to this invoice");
        }
    }

    private String generateInvoiceNumber(LocalDate billingPeriodEnd) {
        return "INV-" + billingPeriodEnd.format(DateTimeFormatter.BASIC_ISO_DATE)
            + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private BigDecimal normalizeMoney(BigDecimal amount) {
        if (amount == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    public InvoiceResponse toInvoiceResponse(Invoice invoice) {
        String subscriptionName = subscriptionRepository.findById(invoice.getSubscriptionId())
            .flatMap(subscription -> apiProductRepository.findById(subscription.getApiProductId())
                .map(ApiProduct::getName))
            .orElse("Unknown API");

        return new InvoiceResponse(
            invoice.getId(),
            invoice.getSubscriptionId(),
            subscriptionName,
            invoice.getInvoiceNumber(),
            invoice.getStatus(),
            invoice.getBillingPeriodStart(),
            invoice.getBillingPeriodEnd(),
            invoice.getBaseAmount(),
            invoice.getTaxAmount(),
            invoice.getTotalAmount(),
            invoice.getDueAt(),
            invoice.getPaidAt(),
            invoice.getPaymentProvider(),
            invoice.getPaymentReference(),
            invoice.getCreatedAt(),
            invoice.getUpdatedAt()
        );
    }

    private UsageResponse toUsageResponse(UsageRecord usageRecord) {
        String subscriptionName = subscriptionRepository.findById(usageRecord.getSubscriptionId())
            .flatMap(subscription -> apiProductRepository.findById(subscription.getApiProductId())
                .map(ApiProduct::getName))
            .orElse("Unknown API");

        return new UsageResponse(
            usageRecord.getId(),
            usageRecord.getSubscriptionId(),
            subscriptionName,
            usageRecord.getUsageDate(),
            usageRecord.getCallCount(),
            usageRecord.getTotalCost(),
            usageRecord.getCreatedAt(),
            usageRecord.getUpdatedAt()
        );
    }
}
