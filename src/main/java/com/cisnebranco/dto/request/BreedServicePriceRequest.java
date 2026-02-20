package com.cisnebranco.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record BreedServicePriceRequest(
        @NotNull
        @DecimalMin("0.01")
        @DecimalMax("99999999.99")
        @Digits(integer = 8, fraction = 2)
        BigDecimal price
) {}
