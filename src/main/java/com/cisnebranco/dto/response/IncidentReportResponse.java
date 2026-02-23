package com.cisnebranco.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record IncidentReportResponse(
    Long id,
    String category,
    String description,
    String reportedBy,
    List<IncidentMediaResponse> media,
    LocalDateTime createdAt
) {}
