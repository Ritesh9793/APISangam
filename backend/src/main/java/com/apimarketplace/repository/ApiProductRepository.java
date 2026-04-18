package com.apimarketplace.repository;

import com.apimarketplace.entity.ApiProduct;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiProductRepository extends JpaRepository<ApiProduct, UUID> {
    List<ApiProduct> findByActiveTrueOrderByCreatedAtDesc();

    List<ApiProduct> findByProviderId(UUID providerId);

    Optional<ApiProduct> findBySlugIgnoreCase(String slug);

    boolean existsBySlugIgnoreCase(String slug);
}
