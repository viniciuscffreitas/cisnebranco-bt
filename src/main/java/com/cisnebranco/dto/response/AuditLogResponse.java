package com.cisnebranco.dto.response;

import java.time.LocalDateTime;

public record AuditLogResponse(
        Long id,
        String action,
        String entityType,
        Long entityId,
        String username,
        String details,
        String ipAddress,
        LocalDateTime createdAt
) {}
