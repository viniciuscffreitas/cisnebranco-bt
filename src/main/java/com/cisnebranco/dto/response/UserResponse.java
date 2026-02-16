package com.cisnebranco.dto.response;

import com.cisnebranco.entity.enums.UserRole;

public record UserResponse(
        Long id,
        String username,
        UserRole role,
        Long groomerId,
        String groomerName,
        boolean active
) {}
