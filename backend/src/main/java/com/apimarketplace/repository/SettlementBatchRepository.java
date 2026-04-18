package com.apimarketplace.repository;

import com.apimarketplace.entity.SettlementBatch;
import com.apimarketplace.entity.enums.SettlementStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SettlementBatchRepository extends JpaRepository<SettlementBatch, UUID> {
    List<SettlementBatch> findByProviderIdOrderByPeriodEndDesc(UUID providerId);

    List<SettlementBatch> findByStatus(SettlementStatus status);

    boolean existsByProviderIdAndPeriodStartAndPeriodEnd(UUID providerId, LocalDate periodStart, LocalDate periodEnd);

    Optional<SettlementBatch> findByProviderIdAndPeriodStartAndPeriodEnd(UUID providerId, LocalDate periodStart, LocalDate periodEnd);
}
