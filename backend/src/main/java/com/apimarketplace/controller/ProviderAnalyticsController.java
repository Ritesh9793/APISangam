package com.apimarketplace.controller;

import com.apimarketplace.dto.analytics.ProviderAnalyticsResponse;
import com.apimarketplace.security.UserPrincipal;
import com.apimarketplace.service.ProviderAnalyticsService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/provider")
public class ProviderAnalyticsController {

    private final ProviderAnalyticsService providerAnalyticsService;

    public ProviderAnalyticsController(ProviderAnalyticsService providerAnalyticsService) {
        this.providerAnalyticsService = providerAnalyticsService;
    }

    @GetMapping("/analytics/overview")
    public ProviderAnalyticsResponse overview(@AuthenticationPrincipal UserPrincipal principal) {
        return providerAnalyticsService.overview(principal);
    }
}
