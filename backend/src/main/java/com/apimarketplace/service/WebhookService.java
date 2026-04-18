package com.apimarketplace.service;

import com.apimarketplace.dto.payments.WebhookPaymentEventRequest;
import com.apimarketplace.dto.billing.InvoiceResponse;
import com.apimarketplace.exception.ApiException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class WebhookService {

    private final PaymentService paymentService;
    private final AuditService auditService;
    private final String webhookSecret;

    public WebhookService(
        PaymentService paymentService,
        AuditService auditService,
        @Value("${RAZORPAY_WEBHOOK_SECRET:}") String webhookSecret
    ) {
        this.paymentService = paymentService;
        this.auditService = auditService;
        this.webhookSecret = webhookSecret;
    }

    public InvoiceResponse handlePaymentEvent(WebhookPaymentEventRequest request) {
        if (webhookSecret != null && !webhookSecret.isBlank() && !verifySignature(request)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid webhook signature");
        }

        String event = request.event().toLowerCase(Locale.ROOT);
        if (event.contains("failed")) {
            auditService.recordSystem("PAYMENT_WEBHOOK_FAILED", "invoice", request.invoiceId().toString(), "Payment webhook failure", request.event());
            throw new ApiException(HttpStatus.BAD_REQUEST, "Payment webhook indicates failure");
        }

        if (event.contains("captured") || event.contains("paid") || event.contains("success")) {
            InvoiceResponse response = paymentService.markInvoicePaid(request.invoiceId(), "RAZORPAY_WEBHOOK", request.paymentId());
            auditService.recordSystem("PAYMENT_WEBHOOK_SUCCESS", "invoice", request.invoiceId().toString(), "Payment webhook success", request.event());
            return response;
        }

        throw new ApiException(HttpStatus.BAD_REQUEST, "Unsupported webhook event");
    }

    private boolean verifySignature(WebhookPaymentEventRequest request) {
        try {
            String payload = request.event() + "|" + request.invoiceId() + "|" + request.orderId() + "|" + request.paymentId();
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String expected = java.util.HexFormat.of().formatHex(digest);
            return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), request.signature().getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to verify webhook signature");
        }
    }
}
