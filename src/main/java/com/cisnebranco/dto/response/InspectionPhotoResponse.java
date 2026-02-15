package com.cisnebranco.dto.response;

public record InspectionPhotoResponse(
        Long id,
        Long technicalOsId,
        String filePath,
        String caption
) {}
