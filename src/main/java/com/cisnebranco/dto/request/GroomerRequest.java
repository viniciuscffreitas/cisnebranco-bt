package com.cisnebranco.dto.request;

import jakarta.validation.constraints.NotBlank;

public record GroomerRequest(
        @NotBlank String name,
        @NotBlank String phone
) {}
