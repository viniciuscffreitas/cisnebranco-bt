package com.cisnebranco.mapper;

import com.cisnebranco.dto.response.PetGroomerViewResponse;
import com.cisnebranco.dto.response.PetResponse;
import com.cisnebranco.entity.Client;
import com.cisnebranco.entity.Pet;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface PetMapper {

    @Mapping(source = "breed.id", target = "breedId")
    @Mapping(source = "breed.name", target = "breedName")
    @Mapping(source = "client.id", target = "clientId")
    @Mapping(source = "client.name", target = "clientName")
    PetResponse toResponse(Pet pet);

    @Mapping(source = "breed.name", target = "breedName")
    @Mapping(source = "client", target = "clientFirstName", qualifiedByName = "extractFirstName")
    PetGroomerViewResponse toGroomerViewResponse(Pet pet);

    @Named("extractFirstName")
    default String extractFirstName(Client client) {
        if (client == null || client.getName() == null || client.getName().isBlank()) {
            return null;
        }
        return client.getName().split(" ")[0];
    }
}
