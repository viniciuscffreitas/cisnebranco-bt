package com.cisnebranco.dto.response;

public record CurrentUserResponse(
        Long id,
        String username,
        String role,
        long groomerId
) {}
