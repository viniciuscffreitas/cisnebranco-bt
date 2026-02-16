package com.cisnebranco.controller;

import com.cisnebranco.dto.response.report.ClientSpendingReport;
import com.cisnebranco.dto.response.report.DailyRevenueReport;
import com.cisnebranco.dto.response.report.GroomerPerformanceReport;
import com.cisnebranco.dto.response.report.OsStatusDistribution;
import com.cisnebranco.dto.response.report.PaymentMethodStats;
import com.cisnebranco.dto.response.report.ServiceTypeReport;
import com.cisnebranco.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
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
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/revenue/daily")
    public ResponseEntity<List<DailyRevenueReport>> getDailyRevenue(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(reportService.getRevenueReport(startDate, endDate));
    }

    @GetMapping("/service-types")
    public ResponseEntity<List<ServiceTypeReport>> getServiceTypeReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(reportService.getServiceTypeReport(startDate, endDate));
    }

    @GetMapping("/clients/top")
    public ResponseEntity<List<ClientSpendingReport>> getTopClients(
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit) {
        return ResponseEntity.ok(reportService.getTopClients(limit));
    }

    @GetMapping("/groomers/performance")
    public ResponseEntity<List<GroomerPerformanceReport>> getGroomerPerformance(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(reportService.getGroomerPerformance(startDate, endDate));
    }

    @GetMapping("/status-distribution")
    public ResponseEntity<List<OsStatusDistribution>> getStatusDistribution() {
        return ResponseEntity.ok(reportService.getStatusDistribution());
    }

    @GetMapping("/payment-methods")
    public ResponseEntity<List<PaymentMethodStats>> getPaymentMethodStats() {
        return ResponseEntity.ok(reportService.getPaymentMethodStats());
    }

    @PostMapping("/refresh")
    public ResponseEntity<Void> refreshReports() {
        reportService.refreshReports();
        return ResponseEntity.noContent().build();
    }
}
