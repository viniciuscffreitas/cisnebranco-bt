package com.cisnebranco.service;

import com.cisnebranco.dto.response.report.ClientSpendingReport;
import com.cisnebranco.dto.response.report.DailyRevenueReport;
import com.cisnebranco.dto.response.report.GroomerPerformanceReport;
import com.cisnebranco.dto.response.report.OsStatusDistribution;
import com.cisnebranco.dto.response.report.PaymentMethodStats;
import com.cisnebranco.dto.response.report.ServiceTypeReport;
import com.cisnebranco.repository.ReportRepository;
import jakarta.persistence.EntityManager;
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
    private final EntityManager entityManager;

    private static final String[] MATERIALIZED_VIEWS = {
            "mv_daily_revenue",
            "mv_service_type_stats",
            "mv_groomer_performance",
            "mv_os_status_distribution",
            "mv_payment_method_stats"
    };

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
    public void refreshMaterializedViews() {
        long startTime = System.currentTimeMillis();
        log.info("Starting scheduled refresh of {} materialized views", MATERIALIZED_VIEWS.length);

        for (String viewName : MATERIALIZED_VIEWS) {
            try {
                entityManager
                        .createNativeQuery("REFRESH MATERIALIZED VIEW CONCURRENTLY " + viewName)
                        .executeUpdate();
                log.info("Materialized view '{}' refreshed successfully", viewName);
            } catch (Exception e) {
                log.error("Failed to refresh materialized view '{}': {}", viewName, e.getMessage(), e);
            }
        }

        long durationMs = System.currentTimeMillis() - startTime;
        log.info("Materialized view refresh cycle completed in {} ms", durationMs);
    }
}
