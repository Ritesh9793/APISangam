package com.apimarketplace.service;

import com.apimarketplace.entity.ApiKey;
import com.apimarketplace.repository.ApiKeyRepository;
import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class ApiKeyLookupCacheService {

    private final ApiKeyRepository apiKeyRepository;

    public ApiKeyLookupCacheService(ApiKeyRepository apiKeyRepository) {
        this.apiKeyRepository = apiKeyRepository;
    }

    @Cacheable(cacheNames = "apiKeyPrefixCandidates", key = "#lookupToken")
    public List<ApiKey> findActiveKeysByPrefix(String lookupToken) {
        return apiKeyRepository.findByKeyPrefixAndActiveTrue(lookupToken);
    }
}
