package com.cisnebranco.service;

import com.cisnebranco.dto.response.report.ClientSpendingReport;
import com.cisnebranco.dto.response.report.DailyRevenueReport;
import com.cisnebranco.dto.response.report.GroomerPerformanceReport;
import com.cisnebranco.dto.response.report.OsStatusDistribution;
import com.cisnebranco.dto.response.report.PaymentMethodStats;
import com.cisnebranco.dto.response.report.ServiceTypeReport;
import com.cisnebranco.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final ReportRepository reportRepository;

    @Transactional(readOnly = true)
    public List<DailyRevenueReport> getRevenueReport(LocalDate startDate, LocalDate endDate) {
        return reportRepository.getRevenueReport(startDate, endDate);
    }

    @Transactional(readOnly = true)
    public List<ServiceTypeReport> getServiceTypeReport(LocalDate startDate, LocalDate endDate) {
        return reportRepository.getServiceTypeReport(startDate, endDate);
    }

    @Transactional(readOnly = true)
    public List<ClientSpendingReport> getTopClients(int limit) {
        return reportRepository.getTopClientsBySpending(limit);
    }

    @Transactional(readOnly = true)
    public List<GroomerPerformanceReport> getGroomerPerformance(LocalDate startDate, LocalDate endDate) {
        return reportRepository.getGroomerPerformanceReport(startDate, endDate);
    }

    @Transactional(readOnly = true)
    public List<OsStatusDistribution> getStatusDistribution() {
        return reportRepository.getOsStatusDistribution();
    }

    @Transactional(readOnly = true)
    public List<PaymentMethodStats> getPaymentMethodStats() {
        return reportRepository.getPaymentMethodStats();
    }

    @Transactional
    @Scheduled(cron = "0 0 * * * *")
    public void refreshReports() {
        log.info("Refreshing materialized report views...");
        reportRepository.refreshMaterializedViews();
        log.info("Materialized report views refreshed successfully");
    }
}
