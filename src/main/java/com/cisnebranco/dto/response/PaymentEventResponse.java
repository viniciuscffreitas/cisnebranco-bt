package com.cisnebranco.dto.response;

import com.cisnebranco.entity.enums.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentEventResponse(
        Long id,
        BigDecimal amount,
        PaymentMethod method,
        String transactionRef,
        String notes,
        String createdByUsername,
        LocalDateTime createdAt
) {}
