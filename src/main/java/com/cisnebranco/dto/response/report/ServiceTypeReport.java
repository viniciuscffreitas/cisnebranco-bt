package com.cisnebranco.dto.response.report;

import java.math.BigDecimal;

public interface ServiceTypeReport {
    Long getServiceTypeId();
    String getServiceName();
    Integer getTotalServices();
    BigDecimal getTotalRevenue();
    BigDecimal getAvgPrice();
}
