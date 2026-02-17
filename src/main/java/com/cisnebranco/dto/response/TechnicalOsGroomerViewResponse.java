package com.cisnebranco.dto.response;

import com.cisnebranco.entity.enums.OsStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record TechnicalOsGroomerViewResponse(
        Long id,
        PetGroomerViewResponse pet,
        OsStatus status,
        BigDecimal totalPrice,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        String notes,
        List<OsServiceItemGroomerResponse> serviceItems,
        List<InspectionPhotoResponse> photos,
        HealthChecklistResponse healthChecklist,
        LocalDateTime createdAt
) {}
