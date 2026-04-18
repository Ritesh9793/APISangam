package com.apimarketplace.service;

import com.apimarketplace.dto.api.ApiProductRequest;
import com.apimarketplace.dto.api.ApiProductResponse;
import com.apimarketplace.entity.ApiProduct;
import com.apimarketplace.entity.UserAccount;
import com.apimarketplace.entity.enums.UserRole;
import com.apimarketplace.exception.ApiException;
import com.apimarketplace.repository.ApiProductRepository;
import com.apimarketplace.repository.UserRepository;
import com.apimarketplace.security.UserPrincipal;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ApiCatalogService {

    private final ApiProductRepository apiProductRepository;
    private final UserRepository userRepository;
    private final ApiSearchService apiSearchService;
    private final KycService kycService;
    private final AuditService auditService;
    private final NotificationService notificationService;

    public ApiCatalogService(
        ApiProductRepository apiProductRepository,
        UserRepository userRepository,
        ApiSearchService apiSearchService,
        KycService kycService,
        AuditService auditService,
        NotificationService notificationService
    ) {
        this.apiProductRepository = apiProductRepository;
        this.userRepository = userRepository;
        this.apiSearchService = apiSearchService;
        this.kycService = kycService;
        this.auditService = auditService;
        this.notificationService = notificationService;
    }

    @Cacheable(
        cacheNames = "apiCatalogList",
        key = "(#category == null ? '' : #category.toLowerCase()) + '|' + (#query == null ? '' : #query.toLowerCase())"
    )
    public List<ApiProductResponse> list(String category, String query) {
        if (StringUtils.hasText(query)) {
            return apiSearchService.search(query, category);
        }

        return apiProductRepository.findByActiveTrueOrderByCreatedAtDesc()
            .stream()
            .filter(product -> category == null || category.isBlank() || product.getCategory().equalsIgnoreCase(category))
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    @Cacheable(cacheNames = "apiProductDetails", key = "#id")
    public ApiProductResponse get(UUID id) {
        return apiSearchService.get(id);
    }

    @Caching(evict = {
        @CacheEvict(cacheNames = "apiCatalogList", allEntries = true),
        @CacheEvict(cacheNames = "apiProductDetails", allEntries = true),
        @CacheEvict(cacheNames = "providerAnalytics", allEntries = true)
    })
    public ApiProductResponse create(UserPrincipal principal, ApiProductRequest request) {
        if (principal.getRole() != UserRole.PROVIDER && principal.getRole() != UserRole.ADMIN) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only providers can publish APIs");
        }
        kycService.requireApproved(principal, "publish APIs");

        ApiProduct product = new ApiProduct();
        product.setProviderId(principal.getId());
        product.setName(request.name().trim());
        product.setSlug(StringUtils.hasText(request.slug()) ? slugify(request.slug()) : slugify(request.name()));
        product.setDescription(request.description().trim());
        product.setCategory(request.category().trim());
        product.setDocumentationUrl(normalizeNullable(request.documentationUrl()));
        product.setOpenApiSpecUrl(normalizeNullable(request.openApiSpecUrl()));
        product.setBasePrice(request.basePrice().setScale(2, java.math.RoundingMode.HALF_UP));
        product.setRequestsPerMinute(request.requestsPerMinute() == null ? 60 : request.requestsPerMinute());
        product.setRegion(normalizeNullable(request.region()));
        product.setActive(true);

        if (apiProductRepository.existsBySlugIgnoreCase(product.getSlug())) {
            throw new ApiException(HttpStatus.CONFLICT, "API slug already exists");
        }

        ApiProduct saved = apiProductRepository.save(product);
        apiSearchService.index(saved);
        auditService.record(principal, "API_PUBLISHED", "api_product", saved.getId().toString(), "SUCCESS", "API product published", saved.getSlug());
        notificationService.notifyUserEmail(
            principal.getId(),
            "API published successfully",
            "Your API " + saved.getName() + " is now live in the marketplace.",
            "API_PUBLISHED"
        );
        return toResponse(saved);
    }

    public ApiProduct load(UUID id) {
        return apiProductRepository.findById(id)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "API product not found"));
    }

    private String slugify(String value) {
        return value.trim()
            .toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]+", "-")
            .replaceAll("^-+", "")
            .replaceAll("-+$", "");
    }

    private String normalizeNullable(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private ApiProductResponse toResponse(ApiProduct product) {
        return new ApiProductResponse(
            product.getId(),
            product.getProviderId(),
            userRepository.findById(product.getProviderId()).map(UserAccount::getFullName).orElse("Unknown provider"),
            product.getName(),
            product.getSlug(),
            product.getDescription(),
            product.getCategory(),
            product.getDocumentationUrl(),
            product.getOpenApiSpecUrl(),
            product.getBasePrice(),
            product.getRequestsPerMinute(),
            product.isActive(),
            product.getRegion(),
            product.getCreatedAt(),
            product.getUpdatedAt()
        );
    }
}
