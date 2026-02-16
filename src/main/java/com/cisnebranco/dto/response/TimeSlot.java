package com.cisnebranco.dto.response;

import java.time.LocalDateTime;

public record TimeSlot(
        LocalDateTime start,
        LocalDateTime end,
        boolean available
) {}
