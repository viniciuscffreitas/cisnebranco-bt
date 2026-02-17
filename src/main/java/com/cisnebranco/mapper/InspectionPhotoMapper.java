package com.cisnebranco.mapper;

import com.cisnebranco.dto.response.InspectionPhotoResponse;
import com.cisnebranco.entity.InspectionPhoto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.nio.file.Paths;

@Mapper(componentModel = "spring")
public interface InspectionPhotoMapper {

    @Mapping(source = "technicalOs.id", target = "technicalOsId")
    @Mapping(target = "url", source = ".", qualifiedByName = "toPhotoUrl")
    InspectionPhotoResponse toResponse(InspectionPhoto photo);

    @Named("toPhotoUrl")
    default String toPhotoUrl(InspectionPhoto photo) {
        if (photo.getFilePath() == null || photo.getTechnicalOs() == null) {
            return null;
        }
        String filename = Paths.get(photo.getFilePath()).getFileName().toString();
        return "/api/photos/" + photo.getTechnicalOs().getId() + "/" + filename;
    }
}
