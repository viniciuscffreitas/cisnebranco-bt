package com.cisnebranco.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ClientRequest(
        @NotBlank String name,
        @NotBlank String phone,
        String email,
        String address
) {}
