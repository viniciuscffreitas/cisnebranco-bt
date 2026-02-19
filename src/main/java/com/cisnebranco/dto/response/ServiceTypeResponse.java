package com.cisnebranco.dto.response;

import java.math.BigDecimal;

public record ServiceTypeResponse(
        Long id,
        String code,
        String name,
        BigDecimal basePrice,
        BigDecimal commissionRate,
        Integer defaultDurationMinutes,
        boolean active
) {}
