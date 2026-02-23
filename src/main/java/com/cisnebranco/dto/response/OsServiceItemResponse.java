package com.cisnebranco.dto.response;

import java.math.BigDecimal;

public record OsServiceItemResponse(
        Long id,
        Long serviceTypeId,
        String serviceTypeName,
        BigDecimal lockedPrice,
        BigDecimal lockedCommissionRate,
        BigDecimal commissionValue,
        Integer defaultDurationMinutes
) {}
