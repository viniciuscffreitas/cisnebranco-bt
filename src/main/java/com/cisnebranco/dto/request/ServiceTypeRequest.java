package com.cisnebranco.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ServiceTypeRequest(
        @Size(max = 30) String code,
        @NotBlank String name,
        @NotNull @PositiveOrZero BigDecimal basePrice,
        @NotNull @Positive @DecimalMax("1.00") BigDecimal commissionRate,
        @Positive Integer defaultDurationMinutes
) {}
