package com.cisnebranco.service;

import com.cisnebranco.dto.request.BatchSyncRequest;
import com.cisnebranco.dto.request.CheckInRequest;
import com.cisnebranco.dto.response.BatchSyncResponse;
import com.cisnebranco.dto.response.BatchSyncResponse.BatchItemResult;
import com.cisnebranco.dto.response.TechnicalOsResponse;
import com.cisnebranco.exception.BusinessException;
import com.cisnebranco.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BatchSyncService {

    private final TechnicalOsService osService;

    public BatchSyncResponse processBatch(BatchSyncRequest request, Long userId) {
        List<BatchItemResult> checkInResults = new ArrayList<>();
        List<BatchItemResult> statusResults = new ArrayList<>();

        if (request.checkIns() != null) {
            for (int i = 0; i < request.checkIns().size(); i++) {
                CheckInRequest checkIn = request.checkIns().get(i);
                try {
                    TechnicalOsResponse os = osService.checkIn(checkIn, userId);
                    checkInResults.add(BatchItemResult.ok(i, os.id()));
                } catch (ResourceNotFoundException | BusinessException e) {
                    log.warn("Batch check-in failed at index {}: {}", i, e.getMessage());
                    checkInResults.add(BatchItemResult.fail(i, e.getMessage()));
                } catch (Exception e) {
                    var prepaid = checkIn.prepaidPayment();
                    log.error("Unexpected error in batch check-in at index {} (petId={}, prepaid={}/{}): {}",
                            i, checkIn.petId(),
                            prepaid != null ? prepaid.amount().toPlainString() : "none",
                            prepaid != null ? prepaid.method() : "—",
                            e.getMessage(), e);
                    checkInResults.add(BatchItemResult.fail(i, "Internal error"));
                }
            }
        }

        if (request.statusUpdates() != null) {
            for (int i = 0; i < request.statusUpdates().size(); i++) {
                var update = request.statusUpdates().get(i);
                try {
                    TechnicalOsResponse os = osService.updateStatus(update.osId(), update.statusUpdate());
                    statusResults.add(BatchItemResult.ok(i, os.id()));
                } catch (ResourceNotFoundException | BusinessException e) {
                    log.warn("Batch status update failed at index {}: {}", i, e.getMessage());
                    statusResults.add(BatchItemResult.fail(i, e.getMessage()));
                } catch (Exception e) {
                    log.error("Unexpected error in batch status update at index {}", i, e);
                    statusResults.add(BatchItemResult.fail(i, "Internal error"));
                }
            }
        }

        return new BatchSyncResponse(checkInResults, statusResults);
    }
}
