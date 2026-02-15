package com.cisnebranco.dto.response;

import com.cisnebranco.entity.enums.Species;

public record BreedResponse(
        Long id,
        String name,
        Species species
) {}
