package com.cisnebranco.mapper;

import com.cisnebranco.dto.request.HealthChecklistRequest;
import com.cisnebranco.dto.response.HealthChecklistResponse;
import com.cisnebranco.entity.HealthChecklist;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface HealthChecklistMapper {

    @Mapping(source = "technicalOs.id", target = "technicalOsId")
    HealthChecklistResponse toResponse(HealthChecklist checklist);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "technicalOs", ignore = true)
    HealthChecklist toEntity(HealthChecklistRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "technicalOs", ignore = true)
    void updateEntity(HealthChecklistRequest request, @MappingTarget HealthChecklist checklist);
}
