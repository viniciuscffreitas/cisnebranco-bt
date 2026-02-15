package com.cisnebranco.dto.response;

import com.cisnebranco.entity.enums.PetSize;
import com.cisnebranco.entity.enums.Species;

import java.math.BigDecimal;

public record PricingMatrixResponse(
        Long id,
        Long serviceTypeId,
        String serviceTypeName,
        Species species,
        PetSize petSize,
        BigDecimal price
) {}
