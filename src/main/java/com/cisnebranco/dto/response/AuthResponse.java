package com.cisnebranco.dto.response;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String role,
        Long groomerId
) {}
