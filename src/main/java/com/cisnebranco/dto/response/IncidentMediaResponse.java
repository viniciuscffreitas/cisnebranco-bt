package com.cisnebranco.dto.response;

import java.time.LocalDateTime;

public record IncidentMediaResponse(
    Long id,
    String filePath,
    String contentType,
    LocalDateTime createdAt
) {}
