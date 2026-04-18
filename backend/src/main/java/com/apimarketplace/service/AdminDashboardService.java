package com.apimarketplace.service;

import com.apimarketplace.dto.admin.AdminActivityResponse;
import com.apimarketplace.dto.admin.AdminOperationsDashboardResponse;
import com.apimarketplace.dto.admin.MetricCountResponse;
import com.apimarketplace.dto.audit.AuditLogResponse;
import com.apimarketplace.dto.notifications.NotificationEventResponse;
import com.apimarketplace.entity.enums.NotificationChannel;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class AdminDashboardService {

    private final AuditService auditService;
    private final NotificationService notificationService;

    public AdminDashboardService(AuditService auditService, NotificationService notificationService) {
        this.auditService = auditService;
        this.notificationService = notificationService;
    }

    public AdminOperationsDashboardResponse summary() {
        List<AuditLogResponse> audits = auditService.listAll();
        List<NotificationEventResponse> notifications = notificationService.listAll();
        Instant cutoff = Instant.now().minus(Duration.ofHours(24));

        long auditLogsLast24h = countRecent(audits, cutoff);
        long notificationsLast24h = countRecent(notifications, cutoff);
        long emailNotifications = notifications.stream().filter(event -> event.channel() == NotificationChannel.EMAIL).count();
        long smsNotifications = notifications.stream().filter(event -> event.channel() == NotificationChannel.SMS).count();

        List<MetricCountResponse> auditEventBreakdown = topCounts(
            audits.stream().collect(Collectors.groupingBy(AuditLogResponse::eventType, Collectors.counting()))
        );

        List<MetricCountResponse> notificationBreakdown = topCounts(
            notifications.stream().collect(Collectors.groupingBy(event -> event.channel().name(), Collectors.counting()))
        );

        long csvExports = audits.stream().filter(event -> "SETTLEMENT_EXPORT_CSV".equalsIgnoreCase(event.eventType())).count();
        long pdfExports = audits.stream().filter(event -> "SETTLEMENT_EXPORT_PDF".equalsIgnoreCase(event.eventType())).count();
        long totalExports = csvExports + pdfExports;
        long exportEventsLast24h = audits.stream()
            .filter(event -> event.eventType() != null && event.eventType().startsWith("SETTLEMENT_EXPORT_"))
            .filter(event -> event.createdAt() != null && event.createdAt().isAfter(cutoff))
            .count();

        List<AdminActivityResponse> recentAuditEvents = audits.stream()
            .limit(5)
            .map(event -> new AdminActivityResponse(
                "AUDIT",
                event.eventType(),
                safeDetails(event.message(), event.metadata()),
                event.occurredAt()
            ))
            .collect(Collectors.toList());

        List<AdminActivityResponse> recentNotifications = notifications.stream()
            .limit(5)
            .map(event -> new AdminActivityResponse(
                "NOTIFICATION",
                event.channel().name(),
                safeDetails(event.subject(), event.recipient()),
                event.deliveredAt() == null ? event.createdAt() : event.deliveredAt()
            ))
            .collect(Collectors.toList());

        return new AdminOperationsDashboardResponse(
            audits.size(),
            auditLogsLast24h,
            auditEventBreakdown,
            notifications.size(),
            notificationsLast24h,
            emailNotifications,
            smsNotifications,
            notificationBreakdown,
            totalExports,
            csvExports,
            pdfExports,
            exportEventsLast24h,
            recentAuditEvents,
            recentNotifications
        );
    }

    private long countRecent(List<?> entries, Instant cutoff) {
        return entries.stream()
            .filter(entry -> {
                if (entry instanceof AuditLogResponse audit) {
                    return audit.createdAt() != null && audit.createdAt().isAfter(cutoff);
                }
                if (entry instanceof NotificationEventResponse notification) {
                    Instant timestamp = notification.deliveredAt() == null ? notification.createdAt() : notification.deliveredAt();
                    return timestamp != null && timestamp.isAfter(cutoff);
                }
                return false;
            })
            .count();
    }

    private List<MetricCountResponse> topCounts(Map<String, Long> counts) {
        return counts.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()))
            .map(entry -> new MetricCountResponse(entry.getKey(), entry.getValue()))
            .collect(Collectors.toList());
    }

    private String safeDetails(String first, String second) {
        String left = first == null ? "" : first;
        String right = second == null ? "" : second;
        if (left.isBlank()) {
            return right;
        }
        if (right.isBlank()) {
            return left;
        }
        return left + " | " + right;
    }
}
