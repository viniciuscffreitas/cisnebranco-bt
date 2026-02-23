package com.cisnebranco.mapper;

import com.cisnebranco.dto.response.IncidentMediaResponse;
import com.cisnebranco.dto.response.IncidentReportResponse;
import com.cisnebranco.entity.IncidentMedia;
import com.cisnebranco.entity.IncidentReport;
import org.mapstruct.Mapper;
import java.util.List;

@Mapper(componentModel = "spring")
public interface IncidentReportMapper {
    IncidentReportResponse toResponse(IncidentReport entity);
    IncidentMediaResponse toMediaResponse(IncidentMedia entity);
    List<IncidentReportResponse> toResponseList(List<IncidentReport> entities);
}
