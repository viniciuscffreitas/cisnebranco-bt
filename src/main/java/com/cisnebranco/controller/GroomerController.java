package com.cisnebranco.controller;

import com.cisnebranco.dto.request.GroomerRequest;
import com.cisnebranco.dto.response.GroomerResponse;
import com.cisnebranco.service.GroomerService;
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
@RequestMapping("/groomers")
@RequiredArgsConstructor
@Tag(name = "Groomers", description = "Groomer management")
public class GroomerController {

    private final GroomerService groomerService;

    @Operation(summary = "List all groomers")
    @GetMapping
    public ResponseEntity<List<GroomerResponse>> findAll() {
        return ResponseEntity.ok(groomerService.findAll());
    }

    @Operation(summary = "List active groomers only")
    @GetMapping("/active")
    public ResponseEntity<List<GroomerResponse>> findActive() {
        return ResponseEntity.ok(groomerService.findActive());
    }

    @Operation(summary = "Find a groomer by ID")
    @GetMapping("/{id}")
    public ResponseEntity<GroomerResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(groomerService.findById(id));
    }

    @Operation(summary = "Create a new groomer")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GroomerResponse> create(@Valid @RequestBody GroomerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(groomerService.create(request));
    }

    @Operation(summary = "Update an existing groomer")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GroomerResponse> update(@PathVariable Long id,
                                                   @Valid @RequestBody GroomerRequest request) {
        return ResponseEntity.ok(groomerService.update(id, request));
    }

    @Operation(summary = "Deactivate a groomer by ID")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        groomerService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
