package com.cisnebranco.dto.request;

import com.cisnebranco.entity.enums.OsStatus;
import jakarta.validation.constraints.NotNull;

public record OsStatusUpdateRequest(
        @NotNull OsStatus status
) {}
