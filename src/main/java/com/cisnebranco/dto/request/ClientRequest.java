package com.cisnebranco.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ClientRequest(
        @NotBlank String name,
        @NotBlank @Pattern(regexp = "\\d{10,11}", message = "Phone must be 10-11 digits (DDD + number)") String phone,
        @Email String email,
        String address
) {}
