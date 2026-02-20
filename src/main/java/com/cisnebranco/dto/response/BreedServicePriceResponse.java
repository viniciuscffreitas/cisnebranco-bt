package com.cisnebranco.dto.response;

import java.math.BigDecimal;

public record BreedServicePriceResponse(
        Long breedId,
        Long serviceTypeId,
        String serviceTypeCode,
        String serviceTypeName,
        Integer defaultDurationMinutes,
        BigDecimal basePrice,
        BigDecimal price
) {}
