package com.cisnebranco.dto.request;

public record HealthChecklistRequest(
        String skinCondition,
        String coatCondition,
        boolean hasFleas,
        boolean hasTicks,
        boolean hasWounds,
        String earCondition,
        String nailCondition,
        String observations
) {}
