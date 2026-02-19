package com.cisnebranco.dto.response;

import java.math.BigDecimal;

public record BreedServicePriceResponse(
        Long serviceTypeId,
        String serviceTypeCode,
        String serviceTypeName,
        Integer defaultDurationMinutes,
        BigDecimal price
) {}
