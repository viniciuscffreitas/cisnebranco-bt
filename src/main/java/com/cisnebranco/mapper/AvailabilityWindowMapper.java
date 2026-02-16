package com.cisnebranco.mapper;

import com.cisnebranco.dto.response.AvailabilityWindowResponse;
import com.cisnebranco.entity.AvailabilityWindow;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AvailabilityWindowMapper {

    @Mapping(source = "groomer.id", target = "groomerId")
    AvailabilityWindowResponse toResponse(AvailabilityWindow window);
}
