-- Daily revenue materialized view
CREATE MATERIALIZED VIEW mv_daily_revenue AS
SELECT
    DATE(delivered_at) AS report_date,
    COUNT(*)::INT AS total_orders,
    COALESCE(SUM(total_price), 0) AS total_revenue,
    COALESCE(SUM(total_commission), 0) AS total_commission,
    COALESCE(SUM(balance), 0) AS total_balance,
    COALESCE(SUM(total_paid), 0) AS total_paid
FROM technical_os
WHERE status = 'DELIVERED' AND delivered_at IS NOT NULL
GROUP BY DATE(delivered_at)
ORDER BY report_date DESC;

CREATE UNIQUE INDEX idx_mv_daily_revenue_date ON mv_daily_revenue(report_date);

-- Service type statistics materialized view
CREATE MATERIALIZED VIEW mv_service_type_stats AS
SELECT
    st.id AS service_type_id,
    st.name AS service_name,
    COUNT(osi.id)::INT AS total_services,
    COALESCE(SUM(osi.locked_price), 0) AS total_revenue,
    COALESCE(AVG(osi.locked_price), 0) AS avg_price
FROM os_service_items osi
JOIN service_types st ON st.id = osi.service_type_id
JOIN technical_os tos ON tos.id = osi.technical_os_id
WHERE tos.status = 'DELIVERED'
GROUP BY st.id, st.name;

CREATE UNIQUE INDEX idx_mv_service_type_stats_id ON mv_service_type_stats(service_type_id);

-- Groomer performance materialized view
CREATE MATERIALIZED VIEW mv_groomer_performance AS
SELECT
    g.id AS groomer_id,
    g.name AS groomer_name,
    COUNT(tos.id)::INT AS total_orders,
    COALESCE(SUM(tos.total_price), 0) AS total_revenue,
    COALESCE(SUM(tos.total_commission), 0) AS total_commission,
    COALESCE(AVG(tos.total_price), 0) AS avg_order_value
FROM groomers g
LEFT JOIN technical_os tos ON tos.groomer_id = g.id AND tos.status = 'DELIVERED'
WHERE g.active = TRUE
GROUP BY g.id, g.name;

CREATE UNIQUE INDEX idx_mv_groomer_performance_id ON mv_groomer_performance(groomer_id);

-- OS status distribution materialized view
CREATE MATERIALIZED VIEW mv_os_status_distribution AS
SELECT
    status,
    COUNT(*)::INT AS order_count,
    COALESCE(SUM(total_price), 0) AS total_value
FROM technical_os
GROUP BY status;

CREATE UNIQUE INDEX idx_mv_os_status_dist ON mv_os_status_distribution(status);

-- Payment method statistics materialized view
CREATE MATERIALIZED VIEW mv_payment_method_stats AS
SELECT
    method,
    COUNT(*)::INT AS transaction_count,
    COALESCE(SUM(amount), 0) AS total_amount,
    COALESCE(AVG(amount), 0) AS avg_transaction
FROM payment_events
WHERE amount > 0
GROUP BY method;

CREATE UNIQUE INDEX idx_mv_payment_method_stats ON mv_payment_method_stats(method);

-- Function to refresh all report views concurrently
CREATE OR REPLACE FUNCTION refresh_all_report_views()
RETURNS void AS $$
BEGIN
    REFRESH MATERIALIZED VIEW CONCURRENTLY mv_daily_revenue;
    REFRESH MATERIALIZED VIEW CONCURRENTLY mv_service_type_stats;
    REFRESH MATERIALIZED VIEW CONCURRENTLY mv_groomer_performance;
    REFRESH MATERIALIZED VIEW CONCURRENTLY mv_os_status_distribution;
    REFRESH MATERIALIZED VIEW CONCURRENTLY mv_payment_method_stats;
END;
$$ LANGUAGE plpgsql;
