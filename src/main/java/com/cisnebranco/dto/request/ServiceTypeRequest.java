package com.cisnebranco.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record ServiceTypeRequest(
        @NotBlank String code,
        @NotBlank String name,
        @NotNull @Positive @DecimalMax("1.00") BigDecimal commissionRate
) {}
