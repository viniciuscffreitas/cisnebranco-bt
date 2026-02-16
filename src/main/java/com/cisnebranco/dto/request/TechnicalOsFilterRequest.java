package com.cisnebranco.dto.request;

import com.cisnebranco.entity.enums.OsStatus;
import com.cisnebranco.entity.enums.PaymentStatus;

import java.time.LocalDateTime;

public record TechnicalOsFilterRequest(
        OsStatus status,
        Long groomerId,
        Long clientId,
        PaymentStatus paymentStatus,
        LocalDateTime startDate,
        LocalDateTime endDate
) {}
