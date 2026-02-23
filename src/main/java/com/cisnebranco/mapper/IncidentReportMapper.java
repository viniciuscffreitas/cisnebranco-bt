package com.cisnebranco.mapper;

import com.cisnebranco.dto.response.IncidentMediaResponse;
import com.cisnebranco.dto.response.IncidentReportResponse;
import com.cisnebranco.entity.IncidentMedia;
import com.cisnebranco.entity.IncidentReport;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.nio.file.Paths;
import java.util.List;

@Mapper(componentModel = "spring")
public interface IncidentReportMapper {
    IncidentReportResponse toResponse(IncidentReport entity);

    @Mapping(target = "url", source = ".", qualifiedByName = "toMediaUrl")
    IncidentMediaResponse toMediaResponse(IncidentMedia entity);

    List<IncidentReportResponse> toResponseList(List<IncidentReport> entities);

    @Named("toMediaUrl")
    default String toMediaUrl(IncidentMedia media) {
        if (media.getFilePath() == null || media.getIncidentReport() == null) {
            return null;
        }
        Long osId = media.getIncidentReport().getTechnicalOs().getId();
        String filename = Paths.get(media.getFilePath()).getFileName().toString();
        return "/photos/incidents/" + osId + "/" + filename;
    }
}
