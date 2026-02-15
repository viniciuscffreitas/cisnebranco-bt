package com.cisnebranco.dto.response;

import com.cisnebranco.entity.enums.PetSize;
import com.cisnebranco.entity.enums.Species;

public record PetGroomerViewResponse(
        Long id,
        String name,
        Species species,
        String breedName,
        PetSize size,
        String notes
) {}
