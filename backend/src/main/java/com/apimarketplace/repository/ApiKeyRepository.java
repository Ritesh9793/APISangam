package com.apimarketplace.repository;

import com.apimarketplace.entity.ApiKey;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiKeyRepository extends JpaRepository<ApiKey, UUID> {
    List<ApiKey> findByOwnerIdOrderByCreatedAtDesc(UUID ownerId);

    List<ApiKey> findBySubscriptionIdOrderByCreatedAtDesc(UUID subscriptionId);

    List<ApiKey> findByKeyPrefixAndActiveTrue(String keyPrefix);
}
