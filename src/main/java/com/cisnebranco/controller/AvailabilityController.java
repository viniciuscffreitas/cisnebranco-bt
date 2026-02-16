package com.cisnebranco.controller;

import com.cisnebranco.dto.request.AvailabilityWindowRequest;
import com.cisnebranco.dto.response.AvailabilityWindowResponse;
import com.cisnebranco.service.AvailabilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/groomers/{groomerId}/availability")
@RequiredArgsConstructor
@Tag(name = "Availability", description = "Groomer availability window management")
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    @Operation(summary = "Create an availability window for a groomer")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AvailabilityWindowResponse> createWindow(
            @PathVariable Long groomerId,
            @Valid @RequestBody AvailabilityWindowRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(availabilityService.createWindow(groomerId, request));
    }

    @Operation(summary = "List availability windows for a groomer")
    @GetMapping
    public ResponseEntity<List<AvailabilityWindowResponse>> getAvailability(@PathVariable Long groomerId) {
        return ResponseEntity.ok(availabilityService.getGroomerAvailability(groomerId));
    }

    @Operation(summary = "Delete an availability window")
    @DeleteMapping("/{windowId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteWindow(@PathVariable Long groomerId, @PathVariable Long windowId) {
        availabilityService.deleteWindow(windowId);
        return ResponseEntity.noContent().build();
    }
}
