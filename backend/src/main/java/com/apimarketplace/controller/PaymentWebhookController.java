package com.apimarketplace.controller;

import com.apimarketplace.dto.billing.InvoiceResponse;
import com.apimarketplace.dto.payments.WebhookPaymentEventRequest;
import com.apimarketplace.service.WebhookService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
public class PaymentWebhookController {

    private final WebhookService webhookService;

    public PaymentWebhookController(WebhookService webhookService) {
        this.webhookService = webhookService;
    }

    @PostMapping("/webhook")
    public InvoiceResponse webhook(@Valid @RequestBody WebhookPaymentEventRequest request) {
        return webhookService.handlePaymentEvent(request);
    }
}
