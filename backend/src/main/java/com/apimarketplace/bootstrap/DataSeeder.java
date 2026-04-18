package com.apimarketplace.bootstrap;

import com.apimarketplace.entity.KycApplication;
import com.apimarketplace.entity.ApiProduct;
import com.apimarketplace.entity.Invoice;
import com.apimarketplace.entity.Subscription;
import com.apimarketplace.entity.UserAccount;
import com.apimarketplace.entity.enums.KycStatus;
import com.apimarketplace.entity.enums.KycVerificationMethod;
import com.apimarketplace.entity.enums.SubscriptionStatus;
import com.apimarketplace.entity.enums.UserRole;
import com.apimarketplace.repository.KycApplicationRepository;
import com.apimarketplace.repository.ApiProductRepository;
import com.apimarketplace.repository.SubscriptionRepository;
import com.apimarketplace.repository.UserRepository;
import com.apimarketplace.service.ApiSearchService;
import com.apimarketplace.service.BillingService;
import com.apimarketplace.service.SettlementService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DataSeeder implements CommandLineRunner {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Kolkata");

    private final UserRepository userRepository;
    private final ApiProductRepository apiProductRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final KycApplicationRepository kycApplicationRepository;
    private final PasswordEncoder passwordEncoder;
    private final BillingService billingService;
    private final ApiSearchService apiSearchService;
    private final SettlementService settlementService;

    public DataSeeder(
        UserRepository userRepository,
        ApiProductRepository apiProductRepository,
        SubscriptionRepository subscriptionRepository,
        KycApplicationRepository kycApplicationRepository,
        PasswordEncoder passwordEncoder,
        BillingService billingService,
        ApiSearchService apiSearchService,
        SettlementService settlementService
    ) {
        this.userRepository = userRepository;
        this.apiProductRepository = apiProductRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.kycApplicationRepository = kycApplicationRepository;
        this.passwordEncoder = passwordEncoder;
        this.billingService = billingService;
        this.apiSearchService = apiSearchService;
        this.settlementService = settlementService;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0 || apiProductRepository.count() > 0) {
            return;
        }

        UserAccount admin = createUser("admin@apimarketplace.local", "Admin@1234", "Platform Admin", "API Marketplace", UserRole.ADMIN);
        UserAccount provider = createUser("provider@apimarketplace.local", "Provider@1234", "Demo Provider", "FinEdge Systems", UserRole.PROVIDER);
        UserAccount consumer = createUser("consumer@apimarketplace.local", "Consumer@1234", "Demo Consumer", "Nova Retail", UserRole.CONSUMER);

        userRepository.saveAll(List.of(admin, provider, consumer));
        kycApplicationRepository.saveAll(List.of(
            createApprovedKyc(provider, admin.getId(), "Demo Provider Private Limited", "Business", "ABCDE1234F", "22ABCDE1234F1Z5", "9876543210", "HDFC0ABC123"),
            createApprovedKyc(consumer, admin.getId(), "Nova Retail LLP", "Retail", "PQRSX6789L", null, "9123456789", "ICIC0XYZ456")
        ));

        ApiProduct gstApi = createApi(provider, "GST Validation API", "gst-validation-api", "Validate GSTIN details for Indian businesses.", "Compliance", new BigDecimal("1999.00"), 120, "India");
        ApiProduct panApi = createApi(provider, "PAN Verification API", "pan-verification-api", "Verify PAN numbers and basic identity signals.", "Compliance", new BigDecimal("1499.00"), 150, "India");
        ApiProduct smsApi = createApi(provider, "SMS Gateway API", "sms-gateway-api", "Send transactional and marketing SMS messages.", "Messaging", new BigDecimal("999.00"), 300, "India");
        List<ApiProduct> savedProducts = apiProductRepository.saveAll(List.of(gstApi, panApi, smsApi));
        apiSearchService.reindexAll();

        Subscription subscription = new Subscription();
        subscription.setConsumerId(consumer.getId());
        subscription.setApiProductId(savedProducts.get(0).getId());
        subscription.setPlanName("Starter");
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setMonthlyPrice(savedProducts.get(0).getBasePrice());
        subscription.setMonthlyRequestLimit(5_000);
        subscriptionRepository.save(subscription);

        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        Invoice invoice = billingService.createInvoiceForSubscription(
            subscription,
            today.withDayOfMonth(1),
            today.withDayOfMonth(today.lengthOfMonth())
        );
        billingService.markInvoicePaid(invoice.getId(), "SEED", "SEED-PAID");
        billingService.recordUsage(subscription.getId(), 240, new BigDecimal("48.00"));
        billingService.recordUsage(subscription.getId(), 120, new BigDecimal("24.00"));
        settlementService.runSettlementCycle(today.withDayOfMonth(1), today);
    }

    private KycApplication createApprovedKyc(
        UserAccount account,
        java.util.UUID reviewerId,
        String legalBusinessName,
        String businessType,
        String panNumber,
        String gstin,
        String phoneNumber,
        String bankIfsc
    ) {
        KycApplication application = new KycApplication();
        application.setUserId(account.getId());
        application.setLegalBusinessName(legalBusinessName);
        application.setContactName(account.getFullName());
        application.setEmail(account.getEmail());
        application.setPhoneNumber(phoneNumber);
        application.setBusinessType(businessType);
        application.setPanNumber(panNumber);
        application.setGstin(gstin);
        application.setBankAccountMasked("****1234");
        application.setBankIfsc(bankIfsc);
        application.setRegisteredAddress(account.getCompanyName());
        application.setStatus(KycStatus.APPROVED);
        application.setPanVerified(true);
        application.setGstinVerified(gstin != null);
        application.setBankVerified(true);
        application.setAadhaarBasicVerified(true);
        application.setAadhaarOcrVerified(true);
        application.setDrivingLicenseVerified(false);
        application.setPassportVerified(false);
        application.setVoterIdVerified(false);
        application.setFaceMatchVerified(false);
        application.setFaceLivenessVerified(false);
        application.setVerificationMethodsCsv(String.join(",", List.of(KycVerificationMethod.BANK_ACCOUNT.name(), KycVerificationMethod.PAN.name(), KycVerificationMethod.GST.name(), KycVerificationMethod.AADHAAR_BASIC.name())));
        application.setVerificationProvider("LOCAL");
        application.setVerificationReference("LOCAL-SEED");
        application.setVerificationSummaryText("Seeded KYC approval for demo accounts");
        application.setVerificationReferenceIdsCsv("LOCAL-SEED-001");
        application.setAadhaarNumberMasked("****1234");
        application.setSupportingDocumentsCsv("pan-card.pdf,bank-proof.pdf");
        application.setReviewerId(reviewerId);
        application.setSubmittedAt(Instant.now());
        application.setReviewedAt(Instant.now());
        application.setSelfieImageUrl("https://example.com/selfie.jpg");
        application.setIdDocumentImageUrl("https://example.com/id.jpg");
        return application;
    }

    private UserAccount createUser(String email, String password, String fullName, String companyName, UserRole role) {
        UserAccount account = new UserAccount();
        account.setEmail(email);
        account.setPasswordHash(passwordEncoder.encode(password));
        account.setFullName(fullName);
        account.setCompanyName(companyName);
        account.setRole(role);
        account.setEnabled(true);
        return account;
    }

    private ApiProduct createApi(
        UserAccount provider,
        String name,
        String slug,
        String description,
        String category,
        BigDecimal basePrice,
        int requestsPerMinute,
        String region
    ) {
        ApiProduct product = new ApiProduct();
        product.setProviderId(provider.getId());
        product.setName(name);
        product.setSlug(slug);
        product.setDescription(description);
        product.setCategory(category);
        product.setBasePrice(basePrice);
        product.setRequestsPerMinute(requestsPerMinute);
        product.setRegion(region);
        product.setActive(true);
        return product;
    }
}
