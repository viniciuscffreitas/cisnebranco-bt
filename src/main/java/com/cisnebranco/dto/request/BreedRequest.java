package com.cisnebranco.dto.request;

import com.cisnebranco.entity.enums.Species;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record BreedRequest(
        @NotBlank String name,
        @NotNull Species species
) {}
