package com.cisnebranco.repository;

import com.cisnebranco.dto.response.report.ClientSpendingReport;
import com.cisnebranco.dto.response.report.DailyRevenueReport;
import com.cisnebranco.dto.response.report.GroomerPerformanceReport;
import com.cisnebranco.dto.response.report.OsStatusDistribution;
import com.cisnebranco.dto.response.report.PaymentMethodStats;
import com.cisnebranco.dto.response.report.ServiceTypeReport;
import com.cisnebranco.entity.TechnicalOs;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ReportRepository extends JpaRepository<TechnicalOs, Long> {

    @Query(value = """
            SELECT DATE(delivered_at) AS reportDate,
                   COUNT(*)::INT AS totalOrders,
                   COALESCE(SUM(total_price), 0) AS totalRevenue,
                   COALESCE(SUM(total_commission), 0) AS totalCommission,
                   COALESCE(SUM(balance), 0) AS totalBalance,
                   COALESCE(SUM(total_paid), 0) AS totalPaid
            FROM technical_os
            WHERE status = 'DELIVERED'
              AND DATE(delivered_at) BETWEEN :startDate AND :endDate
            GROUP BY DATE(delivered_at)
            ORDER BY reportDate DESC
            """, nativeQuery = true)
    List<DailyRevenueReport> getRevenueReport(@Param("startDate") LocalDate startDate,
                                               @Param("endDate") LocalDate endDate);

    @Query(value = """
            SELECT st.id AS serviceTypeId,
                   st.name AS serviceName,
                   COUNT(osi.id)::INT AS totalServices,
                   COALESCE(SUM(osi.locked_price), 0) AS totalRevenue,
                   COALESCE(AVG(osi.locked_price), 0) AS avgPrice
            FROM os_service_items osi
            JOIN service_types st ON st.id = osi.service_type_id
            JOIN technical_os tos ON tos.id = osi.technical_os_id
            WHERE tos.status = 'DELIVERED'
              AND DATE(tos.delivered_at) BETWEEN :startDate AND :endDate
            GROUP BY st.id, st.name
            ORDER BY totalRevenue DESC
            """, nativeQuery = true)
    List<ServiceTypeReport> getServiceTypeReport(@Param("startDate") LocalDate startDate,
                                                  @Param("endDate") LocalDate endDate);

    @Query(value = """
            SELECT c.id AS clientId,
                   c.name AS clientName,
                   COUNT(tos.id)::INT AS totalOrders,
                   COALESCE(SUM(tos.total_price), 0) AS totalSpent,
                   MIN(tos.created_at) AS firstVisit,
                   MAX(tos.created_at) AS lastVisit
            FROM clients c
            JOIN pets p ON p.client_id = c.id
            JOIN technical_os tos ON tos.pet_id = p.id
            WHERE tos.status = 'DELIVERED'
            GROUP BY c.id, c.name
            ORDER BY totalSpent DESC
            LIMIT :lim
            """, nativeQuery = true)
    List<ClientSpendingReport> getTopClientsBySpending(@Param("lim") int limit);

    @Query(value = """
            SELECT g.id AS groomerId,
                   g.name AS groomerName,
                   COUNT(tos.id)::INT AS totalOrders,
                   COALESCE(SUM(tos.total_price), 0) AS totalRevenue,
                   COALESCE(SUM(tos.total_commission), 0) AS totalCommission,
                   COALESCE(AVG(tos.total_price), 0) AS avgOrderValue
            FROM groomers g
            LEFT JOIN technical_os tos ON tos.groomer_id = g.id
                AND tos.status = 'DELIVERED'
                AND DATE(tos.delivered_at) BETWEEN :startDate AND :endDate
            WHERE g.active = TRUE
            GROUP BY g.id, g.name
            ORDER BY totalRevenue DESC
            """, nativeQuery = true)
    List<GroomerPerformanceReport> getGroomerPerformanceReport(@Param("startDate") LocalDate startDate,
                                                               @Param("endDate") LocalDate endDate);

    // Materialized view queries

    @Query(value = """
            SELECT report_date AS reportDate, total_orders AS totalOrders,
                   total_revenue AS totalRevenue, total_commission AS totalCommission,
                   total_balance AS totalBalance, total_paid AS totalPaid
            FROM mv_daily_revenue
            WHERE report_date BETWEEN :startDate AND :endDate
            ORDER BY report_date DESC
            """, nativeQuery = true)
    List<DailyRevenueReport> getDailyRevenueMV(@Param("startDate") LocalDate startDate,
                                                @Param("endDate") LocalDate endDate);

    @Query(value = "SELECT status, order_count AS orderCount, total_value AS totalValue FROM mv_os_status_distribution",
            nativeQuery = true)
    List<OsStatusDistribution> getOsStatusDistribution();

    @Query(value = "SELECT method, transaction_count AS transactionCount, total_amount AS totalAmount, avg_transaction AS avgTransaction FROM mv_payment_method_stats",
            nativeQuery = true)
    List<PaymentMethodStats> getPaymentMethodStats();

    @Query(value = """
            SELECT service_type_id AS serviceTypeId, service_name AS serviceName,
                   total_services AS totalServices, total_revenue AS totalRevenue,
                   avg_price AS avgPrice
            FROM mv_service_type_stats
            ORDER BY total_revenue DESC
            """, nativeQuery = true)
    List<ServiceTypeReport> getServiceTypeStatsMV();

    @Query(value = """
            SELECT groomer_id AS groomerId, groomer_name AS groomerName,
                   total_orders AS totalOrders, total_revenue AS totalRevenue,
                   total_commission AS totalCommission, avg_order_value AS avgOrderValue
            FROM mv_groomer_performance
            ORDER BY total_revenue DESC
            """, nativeQuery = true)
    List<GroomerPerformanceReport> getGroomerPerformanceMV();

    @Modifying
    @Query(value = "SELECT refresh_all_report_views()", nativeQuery = true)
    void refreshMaterializedViews();
}
