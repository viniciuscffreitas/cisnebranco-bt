package com.cisnebranco.dto.response;

import com.cisnebranco.entity.enums.PetSize;
import com.cisnebranco.entity.enums.Species;

public record PetResponse(
        Long id,
        String name,
        Species species,
        Long breedId,
        String breedName,
        PetSize size,
        String notes,
        Long clientId
) {}
