package com.apimarketplace.controller;

import com.apimarketplace.dto.notifications.NotificationEventResponse;
import com.apimarketplace.entity.enums.NotificationChannel;
import com.apimarketplace.entity.enums.NotificationStatus;
import com.apimarketplace.security.UserPrincipal;
import com.apimarketplace.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import java.util.List;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.format.annotation.DateTimeFormat;

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notifications", description = "Notification history filters, pagination, and admin views")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    @Operation(summary = "List my notifications", description = "Returns the current user's notification history.")
    public List<NotificationEventResponse> mine(@AuthenticationPrincipal UserPrincipal principal) {
        return notificationService.listForUser(principal.getId());
    }

    @GetMapping("/page")
    @Operation(
        summary = "Search my notifications",
        description = "Filter notifications with pagination. Example: GET /api/notifications/page?channel=EMAIL&eventType=KYC_SUBMITTED&page=0&size=20"
    )
    public Page<NotificationEventResponse> minePaged(
        @AuthenticationPrincipal UserPrincipal principal,
        @Parameter(description = "Filter by channel. Examples: EMAIL, SMS", example = "EMAIL")
        @RequestParam(required = false) NotificationChannel channel,
        @Parameter(description = "Filter by status. Example: SENT", example = "SENT")
        @RequestParam(required = false) NotificationStatus status,
        @Parameter(description = "Filter by event type. Examples: KYC_SUBMITTED, SETTLEMENT_READY", example = "KYC_SUBMITTED")
        @RequestParam(required = false) String eventType,
        @Parameter(description = "Filter by recipient address or phone number.", example = "customer@example.com")
        @RequestParam(required = false) String recipient,
        @Parameter(description = "Start timestamp in ISO-8601 format. Example: 2026-04-01T00:00:00Z", example = "2026-04-01T00:00:00Z")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
        @Parameter(description = "End timestamp in ISO-8601 format. Example: 2026-04-30T23:59:59Z", example = "2026-04-30T23:59:59Z")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
        @ParameterObject
        @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return notificationService.pageNotifications(principal.getId(), channel, status, eventType, recipient, from, to, pageable);
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all notifications", description = "Admin view of all notification events.")
    public List<NotificationEventResponse> all() {
        return notificationService.listAll();
    }

    @GetMapping("/admin/page")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Search all notifications",
        description = "Admin filter and pagination view. Example: GET /api/notifications/admin/page?channel=SMS&status=SENT&page=0&size=50"
    )
    public Page<NotificationEventResponse> allPaged(
        @Parameter(description = "Filter by user ID. Example: 550e8400-e29b-41d4-a716-446655440000", example = "550e8400-e29b-41d4-a716-446655440000")
        @RequestParam(required = false) UUID userId,
        @Parameter(description = "Filter by channel. Example: EMAIL", example = "EMAIL")
        @RequestParam(required = false) NotificationChannel channel,
        @Parameter(description = "Filter by status. Example: SENT", example = "SENT")
        @RequestParam(required = false) NotificationStatus status,
        @Parameter(description = "Filter by event type. Example: PAYOUT_COMPLETED", example = "PAYOUT_COMPLETED")
        @RequestParam(required = false) String eventType,
        @Parameter(description = "Filter by recipient address or phone number.", example = "provider@example.com")
        @RequestParam(required = false) String recipient,
        @Parameter(description = "Start timestamp in ISO-8601 format. Example: 2026-04-01T00:00:00Z", example = "2026-04-01T00:00:00Z")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
        @Parameter(description = "End timestamp in ISO-8601 format. Example: 2026-04-30T23:59:59Z", example = "2026-04-30T23:59:59Z")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
        @ParameterObject
        @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return notificationService.pageNotifications(userId, channel, status, eventType, recipient, from, to, pageable);
    }
}
