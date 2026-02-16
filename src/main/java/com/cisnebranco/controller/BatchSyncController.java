package com.cisnebranco.controller;

import com.cisnebranco.dto.request.BatchSyncRequest;
import com.cisnebranco.dto.response.BatchSyncResponse;
import com.cisnebranco.service.BatchSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sync")
@RequiredArgsConstructor
@Tag(name = "Batch Sync", description = "Offline-first batch synchronization")
public class BatchSyncController {

    private final BatchSyncService batchSyncService;

    @Operation(summary = "Process a batch of offline operations")
    @PostMapping("/batch")
    public ResponseEntity<BatchSyncResponse> sync(@Valid @RequestBody BatchSyncRequest request) {
        return ResponseEntity.ok(batchSyncService.processBatch(request));
    }
}
