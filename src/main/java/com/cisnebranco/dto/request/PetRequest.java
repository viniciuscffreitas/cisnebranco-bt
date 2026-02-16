package com.cisnebranco.dto.request;

import com.cisnebranco.entity.enums.PetSize;
import com.cisnebranco.entity.enums.Species;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PetRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull Species species,
        Long breedId,
        @NotNull PetSize size,
        @Size(max = 500) String notes,
        @NotNull Long clientId
) {}
