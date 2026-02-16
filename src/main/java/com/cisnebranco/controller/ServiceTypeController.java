package com.cisnebranco.controller;

import com.cisnebranco.dto.request.ServiceTypeRequest;
import com.cisnebranco.dto.response.ServiceTypeResponse;
import com.cisnebranco.service.ServiceTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/service-types")
@RequiredArgsConstructor
@Tag(name = "Service Types", description = "Service type management")
public class ServiceTypeController {

    private final ServiceTypeService serviceTypeService;

    @Operation(summary = "List all service types")
    @GetMapping
    public ResponseEntity<List<ServiceTypeResponse>> findAll() {
        return ResponseEntity.ok(serviceTypeService.findAll());
    }

    @Operation(summary = "Find a service type by ID")
    @GetMapping("/{id}")
    public ResponseEntity<ServiceTypeResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(serviceTypeService.findById(id));
    }

    @Operation(summary = "Create a new service type")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ServiceTypeResponse> create(@Valid @RequestBody ServiceTypeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(serviceTypeService.create(request));
    }

    @Operation(summary = "Update an existing service type")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ServiceTypeResponse> update(@PathVariable Long id,
                                                       @Valid @RequestBody ServiceTypeRequest request) {
        return ResponseEntity.ok(serviceTypeService.update(id, request));
    }
}
