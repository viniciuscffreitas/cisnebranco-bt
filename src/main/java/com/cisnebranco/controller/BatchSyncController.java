package com.cisnebranco.controller;

import com.cisnebranco.dto.request.BatchSyncRequest;
import com.cisnebranco.dto.response.BatchSyncResponse;
import com.cisnebranco.service.BatchSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sync")
@RequiredArgsConstructor
public class BatchSyncController {

    private final BatchSyncService batchSyncService;

    @PostMapping("/batch")
    public ResponseEntity<BatchSyncResponse> sync(@RequestBody BatchSyncRequest request) {
        return ResponseEntity.ok(batchSyncService.processBatch(request));
    }
}
