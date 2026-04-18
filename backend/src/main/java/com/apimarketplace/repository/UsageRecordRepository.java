package com.apimarketplace.repository;

import com.apimarketplace.entity.UsageRecord;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsageRecordRepository extends JpaRepository<UsageRecord, UUID> {
    Optional<UsageRecord> findBySubscriptionIdAndUsageDate(UUID subscriptionId, LocalDate usageDate);

    List<UsageRecord> findBySubscriptionIdIn(Collection<UUID> subscriptionIds);

    List<UsageRecord> findBySubscriptionIdOrderByUsageDateDesc(UUID subscriptionId);
}
