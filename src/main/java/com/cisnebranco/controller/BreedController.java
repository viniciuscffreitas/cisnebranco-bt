package com.cisnebranco.controller;

import com.cisnebranco.dto.request.BreedRequest;
import com.cisnebranco.dto.response.BreedResponse;
import com.cisnebranco.service.BreedService;
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
public class BreedController {

    private final BreedService breedService;

    @GetMapping
    public ResponseEntity<List<BreedResponse>> findAll() {
        return ResponseEntity.ok(breedService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<BreedResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(breedService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BreedResponse> create(@Valid @RequestBody BreedRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(breedService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BreedResponse> update(@PathVariable Long id, @Valid @RequestBody BreedRequest request) {
        return ResponseEntity.ok(breedService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        breedService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
