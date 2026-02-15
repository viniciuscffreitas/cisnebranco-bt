package com.cisnebranco.dto.response;

public record GroomerResponse(
        Long id,
        String name,
        String phone,
        boolean active
) {}
