package com.apimarketplace.controller;

import com.apimarketplace.dto.admin.AdminOperationsDashboardResponse;
import com.apimarketplace.service.AdminDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dashboard")
@Tag(name = "Admin Dashboard", description = "Aggregated operations metrics for audit, notification, and export usage")
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    public AdminDashboardController(AdminDashboardService adminDashboardService) {
        this.adminDashboardService = adminDashboardService;
    }

    @GetMapping("/operations")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Get operations dashboard",
        description = "Summarizes audit, notification, and settlement export activity for admins. Example: GET /api/admin/dashboard/operations"
    )
    public AdminOperationsDashboardResponse operations() {
        return adminDashboardService.summary();
    }
}
