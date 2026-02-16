package com.cisnebranco.dto.request;

import java.time.LocalDateTime;

public record ClientFilterRequest(
        String name,
        String phone,
        LocalDateTime registeredAfter
) {}
