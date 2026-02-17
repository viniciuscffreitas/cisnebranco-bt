package com.cisnebranco.mapper;

import com.cisnebranco.dto.response.PetGroomerViewResponse;
import com.cisnebranco.dto.response.PetResponse;
import com.cisnebranco.entity.Pet;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PetMapper {

    @Mapping(source = "breed.id", target = "breedId")
    @Mapping(source = "breed.name", target = "breedName")
    @Mapping(source = "client.id", target = "clientId")
    @Mapping(source = "client.name", target = "clientName")
    PetResponse toResponse(Pet pet);

    @Mapping(source = "breed.name", target = "breedName")
    @Mapping(target = "clientFirstName", expression = "java(pet.getClient() != null ? pet.getClient().getName().split(\" \")[0] : null)")
    PetGroomerViewResponse toGroomerViewResponse(Pet pet);
}
