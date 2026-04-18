package com.apimarketplace.controller;

import com.apimarketplace.dto.billing.CreatePaymentOrderRequest;
import com.apimarketplace.dto.billing.PaymentOrderResponse;
import com.apimarketplace.dto.billing.PaymentVerificationRequest;
import com.apimarketplace.dto.billing.PaymentVerificationResponse;
import com.apimarketplace.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/create-order")
    public PaymentOrderResponse createOrder(@Valid @RequestBody CreatePaymentOrderRequest request) {
        return paymentService.createOrder(request);
    }

    @PostMapping("/verify")
    public PaymentVerificationResponse verify(@Valid @RequestBody PaymentVerificationRequest request) {
        return paymentService.verifyAndMarkPaid(request);
    }
}
