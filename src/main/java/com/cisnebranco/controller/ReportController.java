package com.cisnebranco.controller;

import com.cisnebranco.dto.response.report.ClientSpendingReport;
import com.cisnebranco.dto.response.report.DailyRevenueReport;
import com.cisnebranco.dto.response.report.GroomerPerformanceReport;
import com.cisnebranco.dto.response.report.OsStatusDistribution;
import com.cisnebranco.dto.response.report.PaymentMethodStats;
import com.cisnebranco.dto.response.report.ServiceTypeReport;
import com.cisnebranco.service.ReportExportService;
import com.cisnebranco.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/reports")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Reports", description = "Business analytics and reporting")
public class ReportController {

    private final ReportService reportService;
    private final ReportExportService exportService;

    // ── JSON endpoints ─────────────────────────────────────────────────

    @Operation(summary = "Get daily revenue report for a date range")
    @GetMapping("/revenue/daily")
    public ResponseEntity<List<DailyRevenueReport>> getDailyRevenue(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(reportService.getRevenueReport(startDate, endDate));
    }

    @Operation(summary = "Get service type usage report for a date range")
    @GetMapping("/service-types")
    public ResponseEntity<List<ServiceTypeReport>> getServiceTypeReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(reportService.getServiceTypeReport(startDate, endDate));
    }

    @Operation(summary = "Get top clients ranked by total spending")
    @GetMapping("/clients/top")
    public ResponseEntity<List<ClientSpendingReport>> getTopClients(
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit) {
        return ResponseEntity.ok(reportService.getTopClients(limit));
    }

    @Operation(summary = "Get groomer performance report for a date range")
    @GetMapping("/groomers/performance")
    public ResponseEntity<List<GroomerPerformanceReport>> getGroomerPerformance(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(reportService.getGroomerPerformance(startDate, endDate));
    }

    @Operation(summary = "Get service order status distribution")
    @GetMapping("/status-distribution")
    public ResponseEntity<List<OsStatusDistribution>> getStatusDistribution() {
        return ResponseEntity.ok(reportService.getStatusDistribution());
    }

    @Operation(summary = "Get payment method usage statistics")
    @GetMapping("/payment-methods")
    public ResponseEntity<List<PaymentMethodStats>> getPaymentMethodStats() {
        return ResponseEntity.ok(reportService.getPaymentMethodStats());
    }

    @Operation(summary = "Refresh materialized report views")
    @PostMapping("/refresh")
    public ResponseEntity<Void> refreshReports() {
        reportService.refreshReports();
        return ResponseEntity.noContent().build();
    }

    // ── CSV export endpoints ───────────────────────────────────────────

    @GetMapping("/revenue/daily/csv")
    public ResponseEntity<byte[]> getDailyRevenueCsv(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        var data = reportService.getRevenueReport(startDate, endDate);
        String csv = exportService.dailyRevenueToCsv(data);
        return csvResponse(csv, "receita-diaria");
    }

    @GetMapping("/service-types/csv")
    public ResponseEntity<byte[]> getServiceTypeReportCsv(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        var data = reportService.getServiceTypeReport(startDate, endDate);
        String csv = exportService.serviceTypeToCsv(data);
        return csvResponse(csv, "tipos-servico");
    }

    @GetMapping("/clients/top/csv")
    public ResponseEntity<byte[]> getTopClientsCsv(
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit) {
        var data = reportService.getTopClients(limit);
        String csv = exportService.clientSpendingToCsv(data);
        return csvResponse(csv, "top-clientes");
    }

    @GetMapping("/groomers/performance/csv")
    public ResponseEntity<byte[]> getGroomerPerformanceCsv(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        var data = reportService.getGroomerPerformance(startDate, endDate);
        String csv = exportService.groomerPerformanceToCsv(data);
        return csvResponse(csv, "desempenho-groomers");
    }

    @GetMapping("/status-distribution/csv")
    public ResponseEntity<byte[]> getStatusDistributionCsv() {
        var data = reportService.getStatusDistribution();
        String csv = exportService.statusDistributionToCsv(data);
        return csvResponse(csv, "distribuicao-status");
    }

    @GetMapping("/payment-methods/csv")
    public ResponseEntity<byte[]> getPaymentMethodStatsCsv() {
        var data = reportService.getPaymentMethodStats();
        String csv = exportService.paymentMethodsToCsv(data);
        return csvResponse(csv, "metodos-pagamento");
    }

    // ── PDF export endpoints ───────────────────────────────────────────

    @GetMapping("/revenue/daily/pdf")
    public ResponseEntity<byte[]> getDailyRevenuePdf(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        var data = reportService.getRevenueReport(startDate, endDate);
        byte[] pdf = exportService.dailyRevenueToPdf(data, startDate, endDate);
        return pdfResponse(pdf, "receita-diaria");
    }

    @GetMapping("/service-types/pdf")
    public ResponseEntity<byte[]> getServiceTypeReportPdf(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        var data = reportService.getServiceTypeReport(startDate, endDate);
        byte[] pdf = exportService.serviceTypeToPdf(data, startDate, endDate);
        return pdfResponse(pdf, "tipos-servico");
    }

    @GetMapping("/clients/top/pdf")
    public ResponseEntity<byte[]> getTopClientsPdf(
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit) {
        var data = reportService.getTopClients(limit);
        byte[] pdf = exportService.clientSpendingToPdf(data);
        return pdfResponse(pdf, "top-clientes");
    }

    @GetMapping("/groomers/performance/pdf")
    public ResponseEntity<byte[]> getGroomerPerformancePdf(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        var data = reportService.getGroomerPerformance(startDate, endDate);
        byte[] pdf = exportService.groomerPerformanceToPdf(data, startDate, endDate);
        return pdfResponse(pdf, "desempenho-groomers");
    }

    // ── Response builders ──────────────────────────────────────────────

    private ResponseEntity<byte[]> csvResponse(String csv, String filenamePrefix) {
        String filename = filenamePrefix + "-" + LocalDate.now() + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csv.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private ResponseEntity<byte[]> pdfResponse(byte[] pdf, String filenamePrefix) {
        String filename = filenamePrefix + "-" + LocalDate.now() + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
