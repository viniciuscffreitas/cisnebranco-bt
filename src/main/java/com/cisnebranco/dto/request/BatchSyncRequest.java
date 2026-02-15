package com.cisnebranco.dto.request;

import java.util.List;

public record BatchSyncRequest(
        List<CheckInRequest> checkIns,
        List<BatchStatusUpdate> statusUpdates
) {
    public record BatchStatusUpdate(
            Long osId,
            OsStatusUpdateRequest statusUpdate
    ) {}
}
