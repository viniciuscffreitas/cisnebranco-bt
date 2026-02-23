package com.cisnebranco.mapper;

import com.cisnebranco.dto.response.OsServiceItemGroomerResponse;
import com.cisnebranco.dto.response.OsServiceItemResponse;
import com.cisnebranco.dto.response.TechnicalOsGroomerViewResponse;
import com.cisnebranco.dto.response.TechnicalOsResponse;
import com.cisnebranco.entity.OsServiceItem;
import com.cisnebranco.entity.TechnicalOs;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {PetMapper.class, GroomerMapper.class, HealthChecklistMapper.class, PaymentEventMapper.class, InspectionPhotoMapper.class, IncidentReportMapper.class})
public interface TechnicalOsMapper {

    @Mapping(target = "scheduledStart", source = "appointment.scheduledStart")
    @Mapping(target = "incidentCount", expression = "java(os.getIncidentReports() != null ? os.getIncidentReports().size() : 0)")
    TechnicalOsResponse toResponse(TechnicalOs os);

    TechnicalOsGroomerViewResponse toGroomerViewResponse(TechnicalOs os);

    @Mapping(source = "serviceType.id", target = "serviceTypeId")
    @Mapping(source = "serviceType.name", target = "serviceTypeName")
    @Mapping(source = "serviceType.defaultDurationMinutes", target = "defaultDurationMinutes")
    OsServiceItemResponse toServiceItemResponse(OsServiceItem item);

    @Mapping(source = "serviceType.name", target = "serviceTypeName")
    OsServiceItemGroomerResponse toServiceItemGroomerResponse(OsServiceItem item);
}
