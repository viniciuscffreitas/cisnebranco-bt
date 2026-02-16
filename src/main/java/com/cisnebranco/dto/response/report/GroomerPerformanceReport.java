package com.cisnebranco.dto.response.report;

import java.math.BigDecimal;

public interface GroomerPerformanceReport {
    Long getGroomerId();
    String getGroomerName();
    Integer getTotalOrders();
    BigDecimal getTotalRevenue();
    BigDecimal getTotalCommission();
    BigDecimal getAvgOrderValue();
}
