package com.cisnebranco.mapper;

import com.cisnebranco.dto.request.ServiceTypeRequest;
import com.cisnebranco.dto.response.ServiceTypeResponse;
import com.cisnebranco.entity.ServiceType;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ServiceTypeMapper {

    ServiceTypeResponse toResponse(ServiceType serviceType);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "active", ignore = true)
    ServiceType toEntity(ServiceTypeRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "active", ignore = true)
    void updateEntity(ServiceTypeRequest request, @MappingTarget ServiceType serviceType);
}
