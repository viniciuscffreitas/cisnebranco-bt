package com.cisnebranco.dto.request;

import com.cisnebranco.entity.enums.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;

public record CheckInRequest(
        @NotNull Long petId,
        Long groomerId,
        @NotEmpty List<Long> serviceTypeIds,
        String notes,
        @Valid PrepaidPaymentRequest prepaidPayment
) {
    public record PrepaidPaymentRequest(
            @NotNull @Positive BigDecimal amount,
            @NotNull PaymentMethod method,
            String transactionRef
    ) {}
}
