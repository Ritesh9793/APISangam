package com.apimarketplace.entity;

import com.apimarketplace.entity.enums.SubscriptionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "subscription")
public class Subscription extends BaseEntity {

    @Column(name = "consumer_id", nullable = false)
    private UUID consumerId;

    @Column(name = "api_product_id", nullable = false)
    private UUID apiProductId;

    @Column(name = "plan_name", nullable = false)
    private String planName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionStatus status = SubscriptionStatus.ACTIVE;

    @Column(name = "monthly_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal monthlyPrice;

    @Column(name = "monthly_request_limit", nullable = false)
    private Integer monthlyRequestLimit;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt = Instant.now();

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    public UUID getConsumerId() {
        return consumerId;
    }

    public void setConsumerId(UUID consumerId) {
        this.consumerId = consumerId;
    }

    public UUID getApiProductId() {
        return apiProductId;
    }

    public void setApiProductId(UUID apiProductId) {
        this.apiProductId = apiProductId;
    }

    public String getPlanName() {
        return planName;
    }

    public void setPlanName(String planName) {
        this.planName = planName;
    }

    public SubscriptionStatus getStatus() {
        return status;
    }

    public void setStatus(SubscriptionStatus status) {
        this.status = status;
    }

    public BigDecimal getMonthlyPrice() {
        return monthlyPrice;
    }

    public void setMonthlyPrice(BigDecimal monthlyPrice) {
        this.monthlyPrice = monthlyPrice;
    }

    public Integer getMonthlyRequestLimit() {
        return monthlyRequestLimit;
    }

    public void setMonthlyRequestLimit(Integer monthlyRequestLimit) {
        this.monthlyRequestLimit = monthlyRequestLimit;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(Instant cancelledAt) {
        this.cancelledAt = cancelledAt;
    }
}
