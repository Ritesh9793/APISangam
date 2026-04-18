package com.apimarketplace.controller;

import com.apimarketplace.dto.settlement.PayoutRecordResponse;
import com.apimarketplace.dto.settlement.SettlementBatchResponse;
import com.apimarketplace.entity.enums.SettlementStatus;
import com.apimarketplace.security.UserPrincipal;
import com.apimarketplace.service.SettlementService;
import java.time.LocalDate;
import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/settlements")
@Tag(name = "Settlements", description = "Settlement batches, exports, and payout operations")
public class SettlementController {

    private final SettlementService settlementService;

    public SettlementController(SettlementService settlementService) {
        this.settlementService = settlementService;
    }

    @GetMapping
    @Operation(summary = "List my settlements", description = "Returns settlement batches for the current user.")
    public List<SettlementBatchResponse> mine(@AuthenticationPrincipal UserPrincipal principal) {
        return settlementService.listForUser(principal);
    }

    @GetMapping("/export.csv")
    @Operation(
        summary = "Export settlements as CSV",
        description = "Downloads a CSV export of settlement batches. Example: GET /api/settlements/export.csv?from=2026-04-01&to=2026-04-30&status=PAID"
    )
    public ResponseEntity<byte[]> exportCsv(
        @AuthenticationPrincipal UserPrincipal principal,
        @Parameter(description = "Start date in ISO format. Example: 2026-04-01", example = "2026-04-01")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @Parameter(description = "End date in ISO format. Example: 2026-04-30", example = "2026-04-30")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
        @Parameter(description = "Filter by settlement status. Example: PAID", example = "PAID")
        @RequestParam(required = false) SettlementStatus status
    ) {
        byte[] data = settlementService.exportCsv(principal, from, to, status);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment().filename("settlements.csv").build());
        return ResponseEntity.ok()
            .headers(headers)
            .contentType(MediaType.parseMediaType("text/csv"))
            .body(data);
    }

    @GetMapping("/export.pdf")
    @Operation(
        summary = "Export settlements as PDF",
        description = "Downloads a formatted PDF settlement statement with totals. Example: GET /api/settlements/export.pdf?from=2026-04-01&to=2026-04-30&status=PAID"
    )
    public ResponseEntity<byte[]> exportPdf(
        @AuthenticationPrincipal UserPrincipal principal,
        @Parameter(description = "Start date in ISO format. Example: 2026-04-01", example = "2026-04-01")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @Parameter(description = "End date in ISO format. Example: 2026-04-30", example = "2026-04-30")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
        @Parameter(description = "Filter by settlement status. Example: PAID", example = "PAID")
        @RequestParam(required = false) SettlementStatus status
    ) {
        byte[] data = settlementService.exportPdf(principal, from, to, status);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment().filename("settlements.pdf").build());
        return ResponseEntity.ok()
            .headers(headers)
            .contentType(MediaType.APPLICATION_PDF)
            .body(data);
    }

    @GetMapping("/payouts")
    @Operation(summary = "List my payouts", description = "Returns payout records for the current user.")
    public List<PayoutRecordResponse> payouts(@AuthenticationPrincipal UserPrincipal principal) {
        return settlementService.listPayoutsForUser(principal);
    }

    @PostMapping("/admin/run")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Run settlement cycle now", description = "Admin trigger for generating settlements and processing pending payouts.")
    public List<PayoutRecordResponse> runNow() {
        LocalDate today = LocalDate.now(java.time.ZoneId.of("Asia/Kolkata"));
        LocalDate periodStart = today.withDayOfMonth(1);
        return settlementService.runSettlementCycle(periodStart, today);
    }
}
