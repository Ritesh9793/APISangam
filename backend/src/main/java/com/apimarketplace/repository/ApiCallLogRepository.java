package com.apimarketplace.repository;

import com.apimarketplace.entity.ApiCallLog;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiCallLogRepository extends JpaRepository<ApiCallLog, UUID> {
    List<ApiCallLog> findBySubscriptionIdOrderByOccurredAtDesc(UUID subscriptionId);

    long countByApiProductIdAndOccurredAtBetween(UUID apiProductId, Instant start, Instant end);
}
