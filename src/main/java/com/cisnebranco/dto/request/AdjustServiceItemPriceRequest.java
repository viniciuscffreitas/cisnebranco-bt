package com.cisnebranco.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record AdjustServiceItemPriceRequest(
        @NotNull
        @DecimalMin(value = "0.01", message = "O preço ajustado deve ser maior que zero")
        @Digits(integer = 8, fraction = 2, message = "O preço deve ter no máximo 8 dígitos inteiros e 2 casas decimais")
        BigDecimal adjustedPrice,

        @Size(max = 500, message = "O motivo não pode exceder 500 caracteres")
        String reason
) {}
