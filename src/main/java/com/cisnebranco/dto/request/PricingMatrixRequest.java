package com.cisnebranco.dto.request;

import com.cisnebranco.entity.enums.PetSize;
import com.cisnebranco.entity.enums.Species;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record PricingMatrixRequest(
        @NotNull Long serviceTypeId,
        @NotNull Species species,
        @NotNull PetSize petSize,
        @NotNull @Positive BigDecimal price
) {}
