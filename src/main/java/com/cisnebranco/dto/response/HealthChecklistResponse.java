package com.cisnebranco.dto.response;

public record HealthChecklistResponse(
        Long id,
        Long technicalOsId,
        String skinCondition,
        String coatCondition,
        boolean hasFleas,
        boolean hasTicks,
        boolean hasWounds,
        String earCondition,
        String nailCondition,
        String observations
) {}
