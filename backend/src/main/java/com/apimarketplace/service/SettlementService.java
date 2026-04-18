package com.apimarketplace.service;

import com.apimarketplace.dto.settlement.PayoutRecordResponse;
import com.apimarketplace.dto.settlement.SettlementBatchResponse;
import com.apimarketplace.entity.ApiProduct;
import com.apimarketplace.entity.Invoice;
import com.apimarketplace.entity.PayoutRecord;
import com.apimarketplace.entity.SettlementBatch;
import com.apimarketplace.entity.Subscription;
import com.apimarketplace.entity.enums.InvoiceStatus;
import com.apimarketplace.entity.enums.PayoutStatus;
import com.apimarketplace.entity.enums.SettlementStatus;
import com.apimarketplace.entity.enums.SubscriptionStatus;
import com.apimarketplace.entity.enums.UserRole;
import com.apimarketplace.exception.ApiException;
import com.apimarketplace.repository.ApiProductRepository;
import com.apimarketplace.repository.InvoiceRepository;
import com.apimarketplace.repository.PayoutRecordRepository;
import com.apimarketplace.repository.SettlementBatchRepository;
import com.apimarketplace.repository.SubscriptionRepository;
import com.apimarketplace.repository.UserRepository;
import com.apimarketplace.security.UserPrincipal;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SettlementService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Kolkata");

    private final InvoiceRepository invoiceRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final ApiProductRepository apiProductRepository;
    private final SettlementBatchRepository settlementBatchRepository;
    private final PayoutRecordRepository payoutRecordRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final BigDecimal platformFeeRate;
    private final BigDecimal gstRate;

    public SettlementService(
        InvoiceRepository invoiceRepository,
        SubscriptionRepository subscriptionRepository,
        ApiProductRepository apiProductRepository,
        SettlementBatchRepository settlementBatchRepository,
        PayoutRecordRepository payoutRecordRepository,
        UserRepository userRepository,
        NotificationService notificationService,
        AuditService auditService,
        @Value("${app.settlement.platform-fee-rate:0.10}") BigDecimal platformFeeRate,
        @Value("${app.business.gst-rate:0.18}") BigDecimal gstRate
    ) {
        this.invoiceRepository = invoiceRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.apiProductRepository = apiProductRepository;
        this.settlementBatchRepository = settlementBatchRepository;
        this.payoutRecordRepository = payoutRecordRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.auditService = auditService;
        this.platformFeeRate = platformFeeRate;
        this.gstRate = gstRate;
    }

    public List<SettlementBatchResponse> listForUser(UserPrincipal principal) {
        return listForUser(principal, null, null, null);
    }

    public List<SettlementBatchResponse> listForUser(UserPrincipal principal, LocalDate from, LocalDate to, SettlementStatus status) {
        if (principal.getRole() == UserRole.ADMIN) {
            return settlementBatchRepository.findAll().stream()
                .filter(batch -> withinRange(batch.getPeriodStart(), batch.getPeriodEnd(), from, to))
                .filter(batch -> status == null || batch.getStatus() == status)
                .sorted(Comparator.comparing(SettlementBatch::getPeriodEnd).reversed())
                .map(this::toResponse)
                .collect(Collectors.toList());
        }

        return settlementBatchRepository.findByProviderIdOrderByPeriodEndDesc(principal.getId()).stream()
            .filter(batch -> withinRange(batch.getPeriodStart(), batch.getPeriodEnd(), from, to))
            .filter(batch -> status == null || batch.getStatus() == status)
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    public List<PayoutRecordResponse> listPayoutsForUser(UserPrincipal principal) {
        if (principal.getRole() == UserRole.ADMIN) {
            return payoutRecordRepository.findAll().stream()
                .sorted(Comparator.comparing(PayoutRecord::getCreatedAt).reversed())
                .map(this::toResponse)
                .collect(Collectors.toList());
        }

        return payoutRecordRepository.findByProviderIdOrderByCreatedAtDesc(principal.getId()).stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    public byte[] exportCsv(UserPrincipal principal, LocalDate from, LocalDate to, SettlementStatus status) {
        List<SettlementBatchResponse> rows = listForUser(principal, from, to, status);
        StringBuilder csv = new StringBuilder();
        csv.append("providerName,periodStart,periodEnd,invoiceCount,grossRevenue,platformFeeRate,platformFee,taxAmount,netPayout,status,payoutReference,generatedAt,paidAt\n");
        for (SettlementBatchResponse row : rows) {
            csv.append(csvValue(row.providerName())).append(',')
                .append(csvValue(row.periodStart())).append(',')
                .append(csvValue(row.periodEnd())).append(',')
                .append(row.invoiceCount()).append(',')
                .append(csvValue(row.grossRevenue())).append(',')
                .append(csvValue(row.platformFeeRate())).append(',')
                .append(csvValue(row.platformFee())).append(',')
                .append(csvValue(row.taxAmount())).append(',')
                .append(csvValue(row.netPayout())).append(',')
                .append(csvValue(row.status())).append(',')
                .append(csvValue(row.payoutReference())).append(',')
                .append(csvValue(row.generatedAt())).append(',')
                .append(csvValue(row.paidAt())).append('\n');
        }
        auditService.record(principal, "SETTLEMENT_EXPORT_CSV", "settlement_export", principal.getId().toString(), "SUCCESS", "Settlement CSV export generated", "rows=" + rows.size());
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    public byte[] exportPdf(UserPrincipal principal, LocalDate from, LocalDate to, SettlementStatus status) {
        List<SettlementBatchResponse> rows = listForUser(principal, from, to, status);
        BigDecimal totalGross = rows.stream().map(SettlementBatchResponse::grossRevenue).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalPlatformFee = rows.stream().map(SettlementBatchResponse::platformFee).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalTax = rows.stream().map(SettlementBatchResponse::taxAmount).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalNet = rows.stream().map(SettlementBatchResponse::netPayout).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
        String title = "Settlement Export";
        List<String> metaLines = buildSettlementPdfMetaLines(principal, from, to, status, rows.size(), totalGross, totalPlatformFee, totalTax, totalNet);
        float margin = 28f;
        float rowHeight = 20f;
        float[] widths = new float[] { 140f, 110f, 55f, 80f, 75f, 75f, 85f, 70f, 96f };
        String[] headers = new String[] { "Provider", "Period", "Invoices", "Gross", "Fee", "Tax", "Net", "Status", "Payout Ref" };
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PdfLayout layout = openPdfPage(document, margin, title, metaLines);
            layout.y = drawTableHeader(layout.contentStream, layout.y, margin, widths, headers, rowHeight);

            if (rows.isEmpty()) {
                layout = ensurePdfPageSpace(document, layout, margin, title, metaLines, widths, headers, rowHeight);
                layout.y = drawMessageRow(layout.contentStream, layout.y, margin, widths, rowHeight, "No settlement records found.", true);
            } else {
                for (SettlementBatchResponse row : rows) {
                    layout = ensurePdfPageSpace(document, layout, margin, title, metaLines, widths, headers, rowHeight);
                    layout.y = drawTableRow(layout.contentStream, layout.y, margin, widths, rowHeight, rowToCells(row), false, false);
                }
            }

            layout = ensurePdfPageSpace(document, layout, margin, title, metaLines, widths, headers, rowHeight);
            layout.y = drawTableRow(
                layout.contentStream,
                layout.y,
                margin,
                widths,
                rowHeight,
                new String[] {
                    "TOTAL",
                    "",
                    String.valueOf(rows.size()),
                    formatMoney(totalGross),
                    formatMoney(totalPlatformFee),
                    formatMoney(totalTax),
                    formatMoney(totalNet),
                    "",
                    ""
                },
                true,
                true
            );

            layout.contentStream.close();
            document.save(outputStream);
            auditService.record(principal, "SETTLEMENT_EXPORT_PDF", "settlement_export", principal.getId().toString(), "SUCCESS", "Settlement PDF export generated", "rows=" + rows.size());
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to generate settlement PDF export");
        }
    }

    @Scheduled(cron = "0 15 2 1 * *")
    @Transactional
    public void generateMonthlySettlements() {
        YearMonth previousMonth = YearMonth.now(BUSINESS_ZONE).minusMonths(1);
        generateSettlementsForPeriod(previousMonth.atDay(1), previousMonth.atEndOfMonth());
    }

    @Transactional
    public List<SettlementBatchResponse> generateSettlementsForPeriod(LocalDate periodStart, LocalDate periodEnd) {
        List<Invoice> paidInvoices = invoiceRepository.findByStatus(InvoiceStatus.PAID).stream()
            .filter(invoice -> invoice.getPaidAt() != null)
            .filter(invoice -> {
                LocalDate paidDate = invoice.getPaidAt().atZone(BUSINESS_ZONE).toLocalDate();
                return !paidDate.isBefore(periodStart) && !paidDate.isAfter(periodEnd);
            })
            .toList();

        Map<UUID, List<Invoice>> invoicesByProvider = paidInvoices.stream()
            .collect(Collectors.groupingBy(this::resolveProviderId));

        return invoicesByProvider.entrySet().stream()
            .map(entry -> createOrLoadBatch(entry.getKey(), entry.getValue(), periodStart, periodEnd))
            .sorted(Comparator.comparing(SettlementBatchResponse::periodEnd).reversed())
            .toList();
    }

    @Transactional
    public List<PayoutRecordResponse> processPendingPayouts() {
        return settlementBatchRepository.findByStatus(SettlementStatus.READY).stream()
            .map(this::processBatch)
            .collect(Collectors.toList());
    }

    public List<PayoutRecordResponse> runSettlementCycle(LocalDate periodStart, LocalDate periodEnd) {
        generateSettlementsForPeriod(periodStart, periodEnd);
        return processPendingPayouts();
    }

    private SettlementBatchResponse createOrLoadBatch(UUID providerId, List<Invoice> invoices, LocalDate periodStart, LocalDate periodEnd) {
        SettlementBatch existing = settlementBatchRepository.findByProviderIdAndPeriodStartAndPeriodEnd(providerId, periodStart, periodEnd).orElse(null);
        if (existing != null) {
            return toResponse(existing);
        }

        BigDecimal grossRevenue = invoices.stream()
            .map(Invoice::getTotalAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(2, RoundingMode.HALF_UP);
        BigDecimal platformFee = grossRevenue.multiply(platformFeeRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal taxAmount = platformFee.multiply(gstRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal netPayout = grossRevenue.subtract(platformFee).subtract(taxAmount).setScale(2, RoundingMode.HALF_UP);

        SettlementBatch batch = new SettlementBatch();
        batch.setProviderId(providerId);
        batch.setPeriodStart(periodStart);
        batch.setPeriodEnd(periodEnd);
        batch.setInvoiceCount(invoices.size());
        batch.setGrossRevenue(grossRevenue);
        batch.setPlatformFeeRate(platformFeeRate.setScale(4, RoundingMode.HALF_UP));
        batch.setPlatformFee(platformFee);
        batch.setTaxAmount(taxAmount);
        batch.setNetPayout(netPayout);
        batch.setStatus(SettlementStatus.READY);
        batch.setGeneratedAt(Instant.now());
        SettlementBatch saved = settlementBatchRepository.save(batch);

        userRepository.findById(providerId).ifPresent(provider ->
            notificationService.sendEmail(
                provider.getId(),
                provider.getEmail(),
                "Settlement ready for " + periodStart + " to " + periodEnd,
                "A settlement batch of " + netPayout + " INR is ready for payout.",
                "SETTLEMENT_READY"
            )
        );
        auditService.recordSystem("SETTLEMENT_READY", "provider", providerId.toString(), "Settlement batch generated", "gross=" + grossRevenue + ",net=" + netPayout);
        return toResponse(saved);
    }

    private PayoutRecordResponse processBatch(SettlementBatch batch) {
        if (payoutRecordRepository.existsBySettlementBatchId(batch.getId())) {
            return payoutRecordRepository.findBySettlementBatchIdOrderByCreatedAtDesc(batch.getId()).stream()
                .findFirst()
                .map(this::toResponse)
                .orElseThrow();
        }

        batch.setStatus(SettlementStatus.PAID);
        batch.setPaidAt(Instant.now());
        batch.setPayoutReference("PAYOUT-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase());
        settlementBatchRepository.save(batch);

        PayoutRecord record = new PayoutRecord();
        record.setSettlementBatchId(batch.getId());
        record.setProviderId(batch.getProviderId());
        record.setAmount(batch.getNetPayout());
        record.setPayoutMode("BANK_TRANSFER");
        record.setStatus(PayoutStatus.COMPLETED);
        record.setProviderReference(batch.getPayoutReference());
        record.setProcessedAt(Instant.now());
        PayoutRecord saved = payoutRecordRepository.save(record);

        userRepository.findById(batch.getProviderId()).ifPresent(provider ->
            notificationService.sendEmail(
                provider.getId(),
                provider.getEmail(),
                "Payout completed",
                "Your payout " + batch.getPayoutReference() + " for " + batch.getNetPayout() + " INR has been completed.",
                "PAYOUT_COMPLETED"
            )
        );
        auditService.recordSystem("PAYOUT_COMPLETED", "settlement", batch.getId().toString(), "Payout completed", batch.getPayoutReference());
        return toResponse(saved);
    }

    private UUID resolveProviderId(Invoice invoice) {
        Subscription subscription = subscriptionRepository.findById(invoice.getSubscriptionId())
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Subscription not found"));
        if (subscription.getStatus() != SubscriptionStatus.ACTIVE && subscription.getStatus() != SubscriptionStatus.CANCELLED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Subscription is not valid for settlement");
        }
        ApiProduct apiProduct = apiProductRepository.findById(subscription.getApiProductId())
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "API product not found"));
        return apiProduct.getProviderId();
    }

    private SettlementBatchResponse toResponse(SettlementBatch batch) {
        return new SettlementBatchResponse(
            batch.getId(),
            batch.getProviderId(),
            userRepository.findById(batch.getProviderId()).map(user -> user.getFullName()).orElse("Unknown provider"),
            batch.getPeriodStart(),
            batch.getPeriodEnd(),
            batch.getInvoiceCount(),
            batch.getGrossRevenue(),
            batch.getPlatformFeeRate(),
            batch.getPlatformFee(),
            batch.getTaxAmount(),
            batch.getNetPayout(),
            batch.getStatus(),
            batch.getPayoutReference(),
            batch.getGeneratedAt(),
            batch.getPaidAt(),
            batch.getCreatedAt(),
            batch.getUpdatedAt()
        );
    }

    private PayoutRecordResponse toResponse(PayoutRecord payoutRecord) {
        return new PayoutRecordResponse(
            payoutRecord.getId(),
            payoutRecord.getSettlementBatchId(),
            payoutRecord.getProviderId(),
            userRepository.findById(payoutRecord.getProviderId()).map(user -> user.getFullName()).orElse("Unknown provider"),
            payoutRecord.getAmount(),
            payoutRecord.getPayoutMode(),
            payoutRecord.getStatus(),
            payoutRecord.getProviderReference(),
            payoutRecord.getProcessedAt(),
            payoutRecord.getCreatedAt(),
            payoutRecord.getUpdatedAt()
        );
    }

    private boolean withinRange(LocalDate periodStart, LocalDate periodEnd, LocalDate from, LocalDate to) {
        if (from != null && periodEnd.isBefore(from)) {
            return false;
        }
        if (to != null && periodStart.isAfter(to)) {
            return false;
        }
        return true;
    }

    private String csvValue(Object value) {
        if (value == null) {
            return "";
        }
        String text = String.valueOf(value).replace("\"", "\"\"");
        if (text.contains(",") || text.contains("\n") || text.contains("\"")) {
            return "\"" + text + "\"";
        }
        return text;
    }

    private List<String> buildSettlementPdfMetaLines(
        UserPrincipal principal,
        LocalDate from,
        LocalDate to,
        SettlementStatus status,
        int rowCount,
        BigDecimal totalGross,
        BigDecimal totalPlatformFee,
        BigDecimal totalTax,
        BigDecimal totalNet
    ) {
        List<String> lines = new ArrayList<>();
        lines.add("Generated for: " + (principal.getRole() == UserRole.ADMIN ? "Admin" : principal.getUsername()));
        lines.add("Filters: " + describeFilterRange(from, to) + " | status=" + (status == null ? "ALL" : status.name()));
        lines.add("Rows: " + rowCount);
        lines.add("Totals: gross=" + formatMoney(totalGross) + " | fee=" + formatMoney(totalPlatformFee) + " | tax=" + formatMoney(totalTax) + " | net=" + formatMoney(totalNet));
        return lines;
    }

    private String describeFilterRange(LocalDate from, LocalDate to) {
        String left = from == null ? "any-start" : from.toString();
        String right = to == null ? "any-end" : to.toString();
        return left + " to " + right;
    }

    private String[] rowToCells(SettlementBatchResponse row) {
        return new String[] {
            row.providerName(),
            row.periodStart() + " to " + row.periodEnd(),
            String.valueOf(row.invoiceCount()),
            formatMoney(row.grossRevenue()),
            formatMoney(row.platformFee()),
            formatMoney(row.taxAmount()),
            formatMoney(row.netPayout()),
            row.status() == null ? "" : row.status().name(),
            row.payoutReference() == null ? "" : row.payoutReference()
        };
    }

    private String formatMoney(BigDecimal value) {
        return value == null ? "0.00" : value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private PdfLayout openPdfPage(PDDocument document, float margin, String title, List<String> metaLines) throws IOException {
        PdfLayout layout = new PdfLayout();
        layout.page = new PDPage(new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth()));
        document.addPage(layout.page);
        layout.contentStream = new PDPageContentStream(document, layout.page);
        layout.y = layout.page.getMediaBox().getHeight() - margin;
        drawPdfHeader(layout, margin, title, metaLines);
        return layout;
    }

    private void drawPdfHeader(PdfLayout layout, float margin, String title, List<String> metaLines) throws IOException {
        float pageWidth = layout.page.getMediaBox().getWidth();
        drawText(layout.contentStream, title, margin, layout.y, PDType1Font.HELVETICA_BOLD, 18f);
        layout.y -= 22f;
        for (String line : metaLines) {
            drawText(layout.contentStream, line, margin, layout.y, PDType1Font.HELVETICA, 10f);
            layout.y -= 12f;
        }
        layout.contentStream.setStrokingColor(180, 180, 180);
        layout.contentStream.moveTo(margin, layout.y - 2f);
        layout.contentStream.lineTo(pageWidth - margin, layout.y - 2f);
        layout.contentStream.stroke();
        layout.y -= 14f;
    }

    private PdfLayout ensurePdfPageSpace(
        PDDocument document,
        PdfLayout layout,
        float margin,
        String title,
        List<String> metaLines,
        float[] widths,
        String[] headers,
        float rowHeight
    ) throws IOException {
        if (layout.y - rowHeight >= margin + 24f) {
            return layout;
        }
        layout.contentStream.close();
        PdfLayout next = openPdfPage(document, margin, title, metaLines);
        next.y = drawTableHeader(next.contentStream, next.y, margin, widths, headers, rowHeight);
        return next;
    }

    private float drawTableHeader(PDPageContentStream contentStream, float cursorY, float margin, float[] widths, String[] headers, float rowHeight) throws IOException {
        float x = margin;
        for (int i = 0; i < headers.length; i++) {
            contentStream.setNonStrokingColor(232, 236, 240);
            contentStream.addRect(x, cursorY - rowHeight, widths[i], rowHeight);
            contentStream.fillAndStroke();
            drawCellText(contentStream, headers[i], x + 4f, cursorY - 13f, widths[i] - 8f, PDType1Font.HELVETICA_BOLD, 9f, false);
            x += widths[i];
        }
        return cursorY - rowHeight;
    }

    private float drawTableRow(
        PDPageContentStream contentStream,
        float cursorY,
        float margin,
        float[] widths,
        float rowHeight,
        String[] cells,
        boolean bold,
        boolean highlight
    ) throws IOException {
        float x = margin;
        for (int i = 0; i < cells.length; i++) {
            if (highlight) {
                contentStream.setNonStrokingColor(244, 248, 252);
                contentStream.addRect(x, cursorY - rowHeight, widths[i], rowHeight);
                contentStream.fillAndStroke();
            } else {
                contentStream.addRect(x, cursorY - rowHeight, widths[i], rowHeight);
                contentStream.stroke();
            }
            boolean rightAlign = i >= 2 && i <= 6;
            float textWidth = widths[i] - 8f;
            float textX = rightAlign ? x + widths[i] - 4f : x + 4f;
            drawCellText(
                contentStream,
                cells[i],
                textX,
                cursorY - 13f,
                textWidth,
                bold ? PDType1Font.HELVETICA_BOLD : PDType1Font.HELVETICA,
                9f,
                rightAlign
            );
            x += widths[i];
        }
        return cursorY - rowHeight;
    }

    private float drawMessageRow(PDPageContentStream contentStream, float cursorY, float margin, float[] widths, float rowHeight, String message, boolean bold) throws IOException {
        float totalWidth = 0f;
        for (float width : widths) {
            totalWidth += width;
        }
        contentStream.addRect(margin, cursorY - rowHeight, totalWidth, rowHeight);
        contentStream.stroke();
        drawCellText(contentStream, message, margin + 6f, cursorY - 13f, totalWidth - 12f, bold ? PDType1Font.HELVETICA_BOLD : PDType1Font.HELVETICA, 9f, false);
        return cursorY - rowHeight;
    }

    private void drawCellText(PDPageContentStream contentStream, String value, float x, float y, float availableWidth, org.apache.pdfbox.pdmodel.font.PDFont font, float fontSize, boolean rightAlign) throws IOException {
        String text = truncateText(normalizePdfText(value), font, fontSize, availableWidth);
        float drawX = rightAlign ? x - stringWidth(font, fontSize, text) : x;
        drawText(contentStream, text, drawX, y, font, fontSize);
    }

    private void drawText(PDPageContentStream contentStream, String text, float x, float y, org.apache.pdfbox.pdmodel.font.PDFont font, float fontSize) throws IOException {
        contentStream.beginText();
        contentStream.setFont(font, fontSize);
        contentStream.newLineAtOffset(x, y);
        contentStream.showText(text == null ? "" : text);
        contentStream.endText();
    }

    private String truncateText(String text, org.apache.pdfbox.pdmodel.font.PDFont font, float fontSize, float maxWidth) throws IOException {
        if (text == null) {
            return "";
        }
        if (stringWidth(font, fontSize, text) <= maxWidth) {
            return text;
        }
        String ellipsis = "...";
        int end = text.length();
        while (end > 0 && stringWidth(font, fontSize, text.substring(0, end) + ellipsis) > maxWidth) {
            end--;
        }
        return end <= 0 ? ellipsis : text.substring(0, end) + ellipsis;
    }

    private float stringWidth(org.apache.pdfbox.pdmodel.font.PDFont font, float fontSize, String text) throws IOException {
        return font.getStringWidth(text) / 1000f * fontSize;
    }

    private String normalizePdfText(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\r", " ").replace("\n", " ").trim();
    }

    private static final class PdfLayout {
        private PDPage page;
        private PDPageContentStream contentStream;
        private float y;
    }
}
