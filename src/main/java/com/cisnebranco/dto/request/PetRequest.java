package com.cisnebranco.dto.request;

import com.cisnebranco.entity.enums.PetSize;
import com.cisnebranco.entity.enums.Species;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PetRequest(
        @NotBlank String name,
        @NotNull Species species,
        Long breedId,
        @NotNull PetSize size,
        String notes,
        @NotNull Long clientId
) {}
