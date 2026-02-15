package com.cisnebranco.exception;

import java.time.LocalDateTime;
import java.util.List;

public record ApiError(
        int status,
        String message,
        List<String> details,
        LocalDateTime timestamp
) {
    public ApiError(int status, String message) {
        this(status, message, List.of(), LocalDateTime.now());
    }

    public ApiError(int status, String message, List<String> details) {
        this(status, message, details, LocalDateTime.now());
    }
}
