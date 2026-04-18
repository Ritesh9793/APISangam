package com.apimarketplace.search;

import java.util.List;
import java.util.UUID;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface ApiProductSearchRepository extends ElasticsearchRepository<ApiProductSearchDocument, UUID> {
    List<ApiProductSearchDocument> findByActiveTrueOrderByUpdatedAtDesc();

    List<ApiProductSearchDocument> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCaseOrCategoryContainingIgnoreCaseOrSlugContainingIgnoreCaseOrProviderNameContainingIgnoreCase(
        String name,
        String description,
        String category,
        String slug,
        String providerName
    );
}
