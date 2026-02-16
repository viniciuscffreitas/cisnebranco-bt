package com.cisnebranco.dto.request;

import com.cisnebranco.entity.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record PaymentRequest(
        @NotNull @Positive BigDecimal amount,
        @NotNull PaymentMethod method,
        String transactionRef,
        String notes
) {}
