package com.apimarketplace.service;

import com.apimarketplace.dto.api.ApiProductResponse;
import com.apimarketplace.entity.ApiProduct;
import com.apimarketplace.entity.UserAccount;
import com.apimarketplace.exception.ApiException;
import com.apimarketplace.repository.ApiProductRepository;
import com.apimarketplace.repository.UserRepository;
import com.apimarketplace.search.ApiProductSearchDocument;
import com.apimarketplace.search.ApiProductSearchRepository;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.ObjectProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ApiSearchService {

    private static final Logger log = LoggerFactory.getLogger(ApiSearchService.class);

    private final ObjectProvider<ApiProductSearchRepository> searchRepositoryProvider;
    private final ApiProductRepository apiProductRepository;
    private final UserRepository userRepository;

    public ApiSearchService(
        ObjectProvider<ApiProductSearchRepository> searchRepositoryProvider,
        ApiProductRepository apiProductRepository,
        UserRepository userRepository
    ) {
        this.searchRepositoryProvider = searchRepositoryProvider;
        this.apiProductRepository = apiProductRepository;
        this.userRepository = userRepository;
    }

    public void index(ApiProduct product) {
        ApiProductSearchRepository searchRepository = searchRepository();
        if (searchRepository == null) {
            return;
        }
        try {
            searchRepository.save(toDocument(product));
        } catch (Exception ex) {
            log.warn("Skipping Elasticsearch indexing for API product {}: {}", product.getId(), ex.getMessage());
        }
    }

    public void reindexAll() {
        ApiProductSearchRepository searchRepository = searchRepository();
        if (searchRepository == null) {
            return;
        }
        try {
            searchRepository.saveAll(apiProductRepository.findAll().stream().map(this::toDocument).toList());
        } catch (Exception ex) {
            log.warn("Skipping Elasticsearch reindex because search backend is unavailable: {}", ex.getMessage());
        }
    }

    public List<ApiProductResponse> search(String query, String category) {
        if (!StringUtils.hasText(query)) {
            return apiProductRepository.findByActiveTrueOrderByCreatedAtDesc()
                .stream()
                .filter(product -> category == null || category.isBlank() || product.getCategory().equalsIgnoreCase(category))
                .map(this::toResponse)
                .collect(Collectors.toList());
        }

        ApiProductSearchRepository searchRepository = searchRepository();
        if (searchRepository == null) {
            return apiProductRepository.findByActiveTrueOrderByCreatedAtDesc()
                .stream()
                .filter(product -> category == null || category.isBlank() || product.getCategory().equalsIgnoreCase(category))
                .filter(product -> matchesQuery(product, query))
                .map(this::toResponse)
                .collect(Collectors.toList());
        }

        try {
            return searchRepository
                .findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCaseOrCategoryContainingIgnoreCaseOrSlugContainingIgnoreCaseOrProviderNameContainingIgnoreCase(
                    query,
                    query,
                    query,
                    query,
                    query
                )
                .stream()
                .filter(ApiProductSearchDocument::isActive)
                .filter(hit -> category == null || category.isBlank() || hit.getCategory().equalsIgnoreCase(category))
                .map(this::toResponse)
                .collect(Collectors.toList());
        } catch (Exception ex) {
            log.warn("Elasticsearch search failed, falling back to database filtering: {}", ex.getMessage());
            return apiProductRepository.findByActiveTrueOrderByCreatedAtDesc()
                .stream()
                .filter(product -> category == null || category.isBlank() || product.getCategory().equalsIgnoreCase(category))
                .filter(product -> matchesQuery(product, query))
                .map(this::toResponse)
                .collect(Collectors.toList());
        }
    }

    public ApiProductResponse get(UUID id) {
        ApiProductSearchRepository searchRepository = searchRepository();
        if (searchRepository == null) {
            ApiProduct product = apiProductRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "API product not found"));
            return toResponse(product);
        }

        try {
            ApiProductSearchDocument document = searchRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "API product not found"));
            return toResponse(document);
        } catch (Exception ex) {
            ApiProduct product = apiProductRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "API product not found"));
            return toResponse(product);
        }
    }

    private boolean matchesQuery(ApiProduct product, String query) {
        String term = query.toLowerCase();
        return product.getName() != null && product.getName().toLowerCase().contains(term)
            || product.getDescription() != null && product.getDescription().toLowerCase().contains(term)
            || product.getCategory() != null && product.getCategory().toLowerCase().contains(term)
            || product.getSlug() != null && product.getSlug().toLowerCase().contains(term);
    }

    private ApiProductSearchDocument toDocument(ApiProduct product) {
        ApiProductSearchDocument document = new ApiProductSearchDocument();
        document.setId(product.getId());
        document.setProviderId(product.getProviderId());
        document.setProviderName(resolveProviderName(product.getProviderId()));
        document.setName(product.getName());
        document.setSlug(product.getSlug());
        document.setDescription(product.getDescription());
        document.setCategory(product.getCategory());
        document.setDocumentationUrl(product.getDocumentationUrl());
        document.setOpenApiSpecUrl(product.getOpenApiSpecUrl());
        document.setBasePrice(product.getBasePrice());
        document.setRequestsPerMinute(product.getRequestsPerMinute());
        document.setActive(product.isActive());
        document.setRegion(product.getRegion());
        document.setCreatedAt(product.getCreatedAt());
        document.setUpdatedAt(product.getUpdatedAt());
        return document;
    }

    private ApiProductResponse toResponse(ApiProductSearchDocument document) {
        return new ApiProductResponse(
            document.getId(),
            document.getProviderId(),
            document.getProviderName(),
            document.getName(),
            document.getSlug(),
            document.getDescription(),
            document.getCategory(),
            document.getDocumentationUrl(),
            document.getOpenApiSpecUrl(),
            document.getBasePrice(),
            document.getRequestsPerMinute(),
            document.isActive(),
            document.getRegion(),
            document.getCreatedAt(),
            document.getUpdatedAt()
        );
    }

    private ApiProductResponse toResponse(ApiProduct product) {
        return new ApiProductResponse(
            product.getId(),
            product.getProviderId(),
            resolveProviderName(product.getProviderId()),
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

    private String resolveProviderName(UUID providerId) {
        return userRepository.findById(providerId)
            .map(UserAccount::getFullName)
            .orElse("Unknown provider");
    }

    private ApiProductSearchRepository searchRepository() {
        return searchRepositoryProvider.getIfAvailable();
    }
}
