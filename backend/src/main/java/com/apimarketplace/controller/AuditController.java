package com.apimarketplace.controller;

import com.apimarketplace.dto.audit.AuditLogResponse;
import com.apimarketplace.security.UserPrincipal;
import com.apimarketplace.service.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.data.domain.Sort;

@RestController
@RequestMapping("/api/audit")
@Tag(name = "Audit", description = "Audit log browsing with filter and pagination examples")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping("/me")
    @Operation(summary = "List my audit logs", description = "Returns the current user's audit trail. Example: GET /api/audit/me")
    public List<AuditLogResponse> mine(@AuthenticationPrincipal UserPrincipal principal) {
        return auditService.listForUser(principal.getId());
    }

    @GetMapping("/page")
    @Operation(
        summary = "Search my audit logs",
        description = "Filter your audit trail with pagination. Example: GET /api/audit/page?eventType=AUTH_LOGIN&status=SUCCESS&page=0&size=20&sort=createdAt,desc"
    )
    public Page<AuditLogResponse> minePaged(
        @AuthenticationPrincipal UserPrincipal principal,
        @Parameter(description = "Filter by actor email. Example: provider@example.com", example = "provider@example.com")
        @RequestParam(required = false) String actorEmail,
        @Parameter(description = "Filter by actor role. Example: ADMIN or PROVIDER", example = "ADMIN")
        @RequestParam(required = false) String actorRole,
        @Parameter(description = "Filter by event type. Examples: AUTH_LOGIN, SETTLEMENT_EXPORT_PDF", example = "SETTLEMENT_EXPORT_PDF")
        @RequestParam(required = false) String eventType,
        @Parameter(description = "Filter by target type. Example: settlement_export", example = "settlement_export")
        @RequestParam(required = false) String targetType,
        @Parameter(description = "Filter by status. Example: SUCCESS", example = "SUCCESS")
        @RequestParam(required = false) String status,
        @Parameter(description = "Start timestamp in ISO-8601 format. Example: 2026-04-01T00:00:00Z", example = "2026-04-01T00:00:00Z")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
        @Parameter(description = "End timestamp in ISO-8601 format. Example: 2026-04-30T23:59:59Z", example = "2026-04-30T23:59:59Z")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
        @ParameterObject
        @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return auditService.pageAuditLogs(principal, actorEmail, actorRole, eventType, targetType, status, from, to, pageable);
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all audit logs", description = "Admin view of the full audit log timeline.")
    public List<AuditLogResponse> all() {
        return auditService.listAll();
    }

    @GetMapping("/admin/page")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Search all audit logs",
        description = "Admin filter and pagination view. Example: GET /api/audit/admin/page?actorRole=PROVIDER&eventType=SETTLEMENT_EXPORT_CSV&page=0&size=50"
    )
    public Page<AuditLogResponse> allPaged(
        @AuthenticationPrincipal UserPrincipal principal,
        @Parameter(description = "Filter by actor email. Example: admin@company.com", example = "admin@company.com")
        @RequestParam(required = false) String actorEmail,
        @Parameter(description = "Filter by actor role. Example: ADMIN", example = "ADMIN")
        @RequestParam(required = false) String actorRole,
        @Parameter(description = "Filter by event type. Example: SETTLEMENT_EXPORT_CSV", example = "SETTLEMENT_EXPORT_CSV")
        @RequestParam(required = false) String eventType,
        @Parameter(description = "Filter by target type. Example: settlement_export", example = "settlement_export")
        @RequestParam(required = false) String targetType,
        @Parameter(description = "Filter by status. Example: SUCCESS", example = "SUCCESS")
        @RequestParam(required = false) String status,
        @Parameter(description = "Start timestamp in ISO-8601 format. Example: 2026-04-01T00:00:00Z", example = "2026-04-01T00:00:00Z")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
        @Parameter(description = "End timestamp in ISO-8601 format. Example: 2026-04-30T23:59:59Z", example = "2026-04-30T23:59:59Z")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
        @ParameterObject
        @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return auditService.pageAuditLogs(principal, actorEmail, actorRole, eventType, targetType, status, from, to, pageable);
    }
}
