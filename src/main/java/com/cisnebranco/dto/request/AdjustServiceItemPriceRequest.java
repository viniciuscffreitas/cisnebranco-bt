package com.cisnebranco.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record AdjustServiceItemPriceRequest(
        @NotNull
        @DecimalMin(value = "0.01", message = "O preço ajustado deve ser maior que zero")
        BigDecimal adjustedPrice
) {}
