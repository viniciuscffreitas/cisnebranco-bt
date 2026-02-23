package com.cisnebranco.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateIncidentRequest(
    @NotNull String category,
    @NotBlank String description
) {}
