package com.apimarketplace.entity;

import com.apimarketplace.entity.enums.PayoutStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payout_record")
public class PayoutRecord extends BaseEntity {

    @Column(name = "settlement_batch_id", nullable = false)
    private UUID settlementBatchId;

    @Column(name = "provider_id", nullable = false)
    private UUID providerId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "payout_mode", nullable = false)
    private String payoutMode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PayoutStatus status = PayoutStatus.QUEUED;

    @Column(name = "provider_reference")
    private String providerReference;

    @Column(name = "processed_at")
    private Instant processedAt;

    public UUID getSettlementBatchId() {
        return settlementBatchId;
    }

    public void setSettlementBatchId(UUID settlementBatchId) {
        this.settlementBatchId = settlementBatchId;
    }

    public UUID getProviderId() {
        return providerId;
    }

    public void setProviderId(UUID providerId) {
        this.providerId = providerId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getPayoutMode() {
        return payoutMode;
    }

    public void setPayoutMode(String payoutMode) {
        this.payoutMode = payoutMode;
    }

    public PayoutStatus getStatus() {
        return status;
    }

    public void setStatus(PayoutStatus status) {
        this.status = status;
    }

    public String getProviderReference() {
        return providerReference;
    }

    public void setProviderReference(String providerReference) {
        this.providerReference = providerReference;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(Instant processedAt) {
        this.processedAt = processedAt;
    }
}
