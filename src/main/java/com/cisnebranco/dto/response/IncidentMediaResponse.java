package com.cisnebranco.dto.response;

import java.time.LocalDateTime;

public record IncidentMediaResponse(
    Long id,
    String url,
    String contentType,
    LocalDateTime createdAt
) {}
