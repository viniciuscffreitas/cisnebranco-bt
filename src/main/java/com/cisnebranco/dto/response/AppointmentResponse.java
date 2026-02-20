package com.cisnebranco.dto.response;

import com.cisnebranco.entity.enums.AppointmentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AppointmentResponse(
        Long id,
        ClientResponse client,
        PetResponse pet,
        GroomerResponse groomer,
        ServiceTypeResponse serviceType,
        LocalDateTime scheduledStart,
        LocalDateTime scheduledEnd,
        AppointmentStatus status,
        String notes,
        Long technicalOsId,
        LocalDateTime cancelledAt,
        String cancellationReason,
        LocalDateTime createdAt,
        BigDecimal estimatedPrice
) {
    public AppointmentResponse withEstimatedPrice(BigDecimal price) {
        return new AppointmentResponse(id, client, pet, groomer, serviceType,
                scheduledStart, scheduledEnd, status, notes, technicalOsId,
                cancelledAt, cancellationReason, createdAt, price);
    }
}
