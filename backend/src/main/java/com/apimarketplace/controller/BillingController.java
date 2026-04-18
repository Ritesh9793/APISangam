package com.apimarketplace.controller;

import com.apimarketplace.dto.billing.InvoiceResponse;
import com.apimarketplace.dto.billing.UsageResponse;
import com.apimarketplace.security.UserPrincipal;
import com.apimarketplace.service.BillingService;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/billing")
public class BillingController {

    private final BillingService billingService;

    public BillingController(BillingService billingService) {
        this.billingService = billingService;
    }

    @GetMapping("/invoices")
    public List<InvoiceResponse> invoices(@AuthenticationPrincipal UserPrincipal principal) {
        return billingService.listInvoicesForUser(principal);
    }

    @GetMapping("/invoices/{id}")
    public InvoiceResponse invoice(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable UUID id
    ) {
        return billingService.getInvoice(id, principal);
    }

    @GetMapping("/usage")
    public List<UsageResponse> usage(@AuthenticationPrincipal UserPrincipal principal) {
        return billingService.listUsageForUser(principal);
    }
}
