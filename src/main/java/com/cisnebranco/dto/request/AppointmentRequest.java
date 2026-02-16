package com.cisnebranco.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record AppointmentRequest(
        @NotNull Long clientId,
        @NotNull Long petId,
        @NotNull Long groomerId,
        @NotNull Long serviceTypeId,
        @NotNull LocalDateTime scheduledStart,
        String notes
) {}
