package com.cisnebranco.dto.request;

import com.cisnebranco.entity.enums.AppointmentStatus;

import java.time.LocalDateTime;

public record AppointmentUpdateRequest(
        LocalDateTime scheduledStart,
        AppointmentStatus status,
        String notes,
        String cancellationReason
) {}
