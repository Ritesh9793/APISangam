package com.apimarketplace.repository;

import com.apimarketplace.entity.Subscription;
import com.apimarketplace.entity.enums.SubscriptionStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {
    List<Subscription> findByConsumerIdOrderByCreatedAtDesc(UUID consumerId);

    List<Subscription> findByApiProductIdIn(Collection<UUID> apiProductIds);

    List<Subscription> findByStatus(SubscriptionStatus status);

    Optional<Subscription> findByIdAndConsumerId(UUID id, UUID consumerId);

    Optional<Subscription> findByConsumerIdAndApiProductId(UUID consumerId, UUID apiProductId);
}
