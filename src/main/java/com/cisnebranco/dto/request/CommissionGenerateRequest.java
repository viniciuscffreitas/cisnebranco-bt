package com.cisnebranco.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CommissionGenerateRequest(
        @NotNull Long groomerId,
        @NotNull LocalDate weekStart
) {}
