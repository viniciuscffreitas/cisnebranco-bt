package com.cisnebranco.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record GroomerRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Pattern(regexp = "\\d{10,11}", message = "Phone must be 10-11 digits (DDD + number)") String phone
) {}
