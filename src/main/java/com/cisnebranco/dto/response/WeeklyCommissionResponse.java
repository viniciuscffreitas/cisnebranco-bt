package com.cisnebranco.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record WeeklyCommissionResponse(
        Long id,
        Long groomerId,
        String groomerName,
        LocalDate weekStart,
        LocalDate weekEnd,
        int totalServices,
        BigDecimal totalRevenue,
        BigDecimal totalCommission
) {}
