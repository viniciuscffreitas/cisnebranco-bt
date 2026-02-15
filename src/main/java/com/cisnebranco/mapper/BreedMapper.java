package com.cisnebranco.mapper;

import com.cisnebranco.dto.request.BreedRequest;
import com.cisnebranco.dto.response.BreedResponse;
import com.cisnebranco.entity.Breed;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface BreedMapper {

    BreedResponse toResponse(Breed breed);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Breed toEntity(BreedRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(BreedRequest request, @MappingTarget Breed breed);
}
