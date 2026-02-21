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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final ReportRepository reportRepository;
    private final JdbcTemplate jdbcTemplate;

    // Guard against overlapping runs — relevant when virtual threads are enabled
    // (spring.threads.virtual.enabled=true removes single-thread serialization).
    private final AtomicBoolean refreshInProgress = new AtomicBoolean(false);

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

    // REFRESH MATERIALIZED VIEW CONCURRENTLY cannot run inside a transaction block.
    // JdbcTemplate.execute() runs with autocommit, satisfying PostgreSQL's requirement.
    // Returns the number of views that failed to refresh (0 = all succeeded).
    @Scheduled(cron = "0 0 * * * *")
    public int refreshMaterializedViews() {
        if (!refreshInProgress.compareAndSet(false, true)) {
            log.warn("Skipping materialized view refresh — previous run still in progress");
            return 0;
        }
        long startTime = System.currentTimeMillis();
        log.info("Starting scheduled refresh of {} materialized views", MATERIALIZED_VIEWS.length);
        int failures = 0;

        try {
            for (String viewName : MATERIALIZED_VIEWS) {
                try {
                    jdbcTemplate.execute("REFRESH MATERIALIZED VIEW CONCURRENTLY " + viewName);
                    log.info("Materialized view '{}' refreshed successfully", viewName);
                } catch (org.springframework.dao.DataAccessException e) {
                    failures++;
                    log.error("Failed to refresh materialized view '{}': {}", viewName, e.getMessage(), e);
                }
            }
        } finally {
            refreshInProgress.set(false);
        }

        long durationMs = System.currentTimeMillis() - startTime;
        if (failures > 0) {
            log.error("Materialized view refresh finished with {}/{} failures in {} ms",
                    failures, MATERIALIZED_VIEWS.length, durationMs);
        } else {
            log.info("Materialized view refresh cycle completed in {} ms", durationMs);
        }
        return failures;
    }
}
