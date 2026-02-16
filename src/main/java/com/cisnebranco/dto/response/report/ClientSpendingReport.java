package com.cisnebranco.dto.response.report;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface ClientSpendingReport {
    Long getClientId();
    String getClientName();
    Integer getTotalOrders();
    BigDecimal getTotalSpent();
    LocalDateTime getFirstVisit();
    LocalDateTime getLastVisit();
}
