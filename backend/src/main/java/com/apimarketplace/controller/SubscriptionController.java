package com.apimarketplace.controller;

import com.apimarketplace.dto.subscription.SubscribeRequest;
import com.apimarketplace.dto.subscription.SubscriptionResponse;
import com.apimarketplace.security.UserPrincipal;
import com.apimarketplace.service.SubscriptionService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/subscriptions")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @GetMapping
    public List<SubscriptionResponse> list(@AuthenticationPrincipal UserPrincipal principal) {
        return subscriptionService.listForUser(principal);
    }

    @PostMapping
    public SubscriptionResponse subscribe(
        @AuthenticationPrincipal UserPrincipal principal,
        @Valid @RequestBody SubscribeRequest request
    ) {
        return subscriptionService.subscribe(principal, request);
    }

    @DeleteMapping("/{id}")
    public SubscriptionResponse cancel(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable UUID id
    ) {
        return subscriptionService.cancel(principal, id);
    }
}
