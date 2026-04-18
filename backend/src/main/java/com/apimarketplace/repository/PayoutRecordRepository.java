package com.apimarketplace.repository;

import com.apimarketplace.entity.PayoutRecord;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PayoutRecordRepository extends JpaRepository<PayoutRecord, UUID> {
    List<PayoutRecord> findByProviderIdOrderByCreatedAtDesc(UUID providerId);

    List<PayoutRecord> findBySettlementBatchIdOrderByCreatedAtDesc(UUID settlementBatchId);

    boolean existsBySettlementBatchId(UUID settlementBatchId);
}
