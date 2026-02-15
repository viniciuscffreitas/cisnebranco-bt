package com.cisnebranco.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record ServiceTypeRequest(
        @NotBlank String code,
        @NotBlank String name,
        @NotNull @Positive BigDecimal commissionRate
) {}
