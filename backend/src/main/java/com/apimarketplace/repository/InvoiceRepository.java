package com.apimarketplace.repository;

import com.apimarketplace.entity.Invoice;
import com.apimarketplace.entity.enums.InvoiceStatus;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {
    List<Invoice> findBySubscriptionIdOrderByBillingPeriodEndDesc(UUID subscriptionId);

    List<Invoice> findBySubscriptionIdIn(Collection<UUID> subscriptionIds);

    List<Invoice> findByStatus(InvoiceStatus status);

    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);

    Optional<Invoice> findBySubscriptionIdAndBillingPeriodStartAndBillingPeriodEnd(
        UUID subscriptionId,
        LocalDate billingPeriodStart,
        LocalDate billingPeriodEnd
    );

    boolean existsBySubscriptionIdAndBillingPeriodStartAndBillingPeriodEnd(
        UUID subscriptionId,
        LocalDate billingPeriodStart,
        LocalDate billingPeriodEnd
    );
}
