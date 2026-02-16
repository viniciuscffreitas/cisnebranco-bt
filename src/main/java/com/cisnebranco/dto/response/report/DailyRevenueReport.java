package com.cisnebranco.dto.response.report;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface DailyRevenueReport {
    LocalDate getReportDate();
    Integer getTotalOrders();
    BigDecimal getTotalRevenue();
    BigDecimal getTotalCommission();
    BigDecimal getTotalBalance();
    BigDecimal getTotalPaid();
}
