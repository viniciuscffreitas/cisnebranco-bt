package com.cisnebranco.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record BatchSyncRequest(
        List<@Valid CheckInRequest> checkIns,
        List<@Valid BatchStatusUpdate> statusUpdates
) {
    public record BatchStatusUpdate(
            @NotNull Long osId,
            @Valid @NotNull OsStatusUpdateRequest statusUpdate
    ) {}
}
