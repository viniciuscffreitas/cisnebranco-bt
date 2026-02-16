package com.cisnebranco.dto.response.report;

import java.math.BigDecimal;

public interface PaymentMethodStats {
    String getMethod();
    Integer getTransactionCount();
    BigDecimal getTotalAmount();
    BigDecimal getAvgTransaction();
}
