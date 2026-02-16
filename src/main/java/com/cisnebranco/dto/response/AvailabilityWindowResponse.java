package com.cisnebranco.dto.response;

import java.time.LocalTime;

public record AvailabilityWindowResponse(
        Long id,
        Long groomerId,
        Integer dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        boolean active
) {}
