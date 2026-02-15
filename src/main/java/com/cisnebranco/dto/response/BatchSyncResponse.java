package com.cisnebranco.dto.response;

import java.util.List;

public record BatchSyncResponse(
        List<BatchItemResult> checkInResults,
        List<BatchItemResult> statusUpdateResults
) {
    public record BatchItemResult(
            int index,
            boolean success,
            Long osId,
            String error
    ) {
        public static BatchItemResult ok(int index, Long osId) {
            return new BatchItemResult(index, true, osId, null);
        }

        public static BatchItemResult fail(int index, String error) {
            return new BatchItemResult(index, false, null, error);
        }
    }
}
