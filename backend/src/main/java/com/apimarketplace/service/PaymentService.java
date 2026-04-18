package com.apimarketplace.service;

import com.apimarketplace.dto.billing.CreatePaymentOrderRequest;
import com.apimarketplace.dto.billing.InvoiceResponse;
import com.apimarketplace.dto.billing.PaymentOrderResponse;
import com.apimarketplace.dto.billing.PaymentVerificationRequest;
import com.apimarketplace.dto.billing.PaymentVerificationResponse;
import com.apimarketplace.entity.Invoice;
import com.apimarketplace.entity.enums.InvoiceStatus;
import com.apimarketplace.exception.ApiException;
import com.apimarketplace.repository.InvoiceRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class PaymentService {

    private final InvoiceRepository invoiceRepository;
    private final BillingService billingService;
    private final String upiMerchantId;
    private final String razorpayKeySecret;

    public PaymentService(
        InvoiceRepository invoiceRepository,
        BillingService billingService,
        @Value("${app.payment.upi-merchant-id:apimarketplace}") String upiMerchantId,
        @Value("${RAZORPAY_KEY_SECRET:}") String razorpayKeySecret
    ) {
        this.invoiceRepository = invoiceRepository;
        this.billingService = billingService;
        this.upiMerchantId = upiMerchantId;
        this.razorpayKeySecret = razorpayKeySecret;
    }

    public PaymentOrderResponse createOrder(CreatePaymentOrderRequest request) {
        Invoice invoice = invoiceRepository.findById(request.invoiceId())
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Invoice not found"));

        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new ApiException(HttpStatus.CONFLICT, "Invoice is already paid");
        }

        String currency = request.currency() == null || request.currency().isBlank()
            ? "INR"
            : request.currency().trim().toUpperCase();
        long amountInPaise = invoice.getTotalAmount().movePointRight(2).longValueExact();
        String orderId = "order_" + UUID.randomUUID().toString().replace("-", "").substring(0, 18);
        String upiIntent = buildUpiIntent(invoice.getTotalAmount(), orderId);

        return new PaymentOrderResponse(orderId, currency, amountInPaise, upiIntent, invoice.getId());
    }

    public PaymentVerificationResponse verifyAndMarkPaid(PaymentVerificationRequest request) {
        if (!verifyPaymentSignature(request.orderId(), request.paymentId(), request.signature())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid payment signature");
        }

        Invoice invoice = invoiceRepository.findById(request.invoiceId())
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Invoice not found"));
        billingService.markInvoicePaid(invoice.getId(), "RAZORPAY", request.paymentId());
        return new PaymentVerificationResponse(true, request.paymentId(), invoice.getId());
    }

    @CacheEvict(cacheNames = "providerAnalytics", allEntries = true)
    public InvoiceResponse markInvoicePaid(UUID invoiceId, String paymentProvider, String paymentReference) {
        return billingService.markInvoicePaid(invoiceId, paymentProvider, paymentReference);
    }

    public boolean verifyPaymentSignature(String orderId, String paymentId, String signature) {
        if (razorpayKeySecret == null || razorpayKeySecret.isBlank()) {
            return false;
        }

        String payload = orderId + "|" + paymentId;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(razorpayKeySecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String expected = HexFormat.of().formatHex(digest);
            return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), signature.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to verify payment signature");
        }
    }

    private String buildUpiIntent(BigDecimal amount, String orderId) {
        return String.format(
            "upi://pay?pa=%s@upi&pn=API%%20Marketplace&am=%s&tr=%s&cu=INR",
            upiMerchantId,
            amount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString(),
            orderId
        );
    }
}
