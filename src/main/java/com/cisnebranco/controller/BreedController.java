package com.cisnebranco.controller;

import com.cisnebranco.dto.request.BreedRequest;
import com.cisnebranco.dto.response.BreedResponse;
import com.cisnebranco.service.BreedService;
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
@RequestMapping("/breeds")
@RequiredArgsConstructor
@Tag(name = "Breeds", description = "Pet breed management")
public class BreedController {

    private final BreedService breedService;

    @Operation(summary = "List all breeds")
    @GetMapping
    public ResponseEntity<List<BreedResponse>> findAll() {
        return ResponseEntity.ok(breedService.findAll());
    }

    @Operation(summary = "Find a breed by ID")
    @GetMapping("/{id}")
    public ResponseEntity<BreedResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(breedService.findById(id));
    }

    @Operation(summary = "Create a new breed")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BreedResponse> create(@Valid @RequestBody BreedRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(breedService.create(request));
    }

    @Operation(summary = "Update an existing breed")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BreedResponse> update(@PathVariable Long id, @Valid @RequestBody BreedRequest request) {
        return ResponseEntity.ok(breedService.update(id, request));
    }

    @Operation(summary = "Delete a breed by ID")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        breedService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
