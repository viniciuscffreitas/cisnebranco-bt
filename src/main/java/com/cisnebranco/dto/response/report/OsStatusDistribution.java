package com.cisnebranco.dto.response.report;

import java.math.BigDecimal;

public interface OsStatusDistribution {
    String getStatus();
    Integer getOrderCount();
    BigDecimal getTotalValue();
}
