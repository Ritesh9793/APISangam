package com.apimarketplace.dto.admin;

import java.util.List;

public record AdminOperationsDashboardResponse(
    long totalAuditLogs,
    long auditLogsLast24h,
    List<MetricCountResponse> auditEventBreakdown,
    long totalNotifications,
    long notificationsLast24h,
    long emailNotifications,
    long smsNotifications,
    List<MetricCountResponse> notificationBreakdown,
    long totalExports,
    long csvExports,
    long pdfExports,
    long exportEventsLast24h,
    List<AdminActivityResponse> recentAuditEvents,
    List<AdminActivityResponse> recentNotifications
) {}
