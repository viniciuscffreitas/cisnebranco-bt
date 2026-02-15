package com.cisnebranco.controller;

import com.cisnebranco.dto.request.GroomerRequest;
import com.cisnebranco.dto.response.GroomerResponse;
import com.cisnebranco.service.GroomerService;
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
public class GroomerController {

    private final GroomerService groomerService;

    @GetMapping
    public ResponseEntity<List<GroomerResponse>> findAll() {
        return ResponseEntity.ok(groomerService.findAll());
    }

    @GetMapping("/active")
    public ResponseEntity<List<GroomerResponse>> findActive() {
        return ResponseEntity.ok(groomerService.findActive());
    }

    @GetMapping("/{id}")
    public ResponseEntity<GroomerResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(groomerService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GroomerResponse> create(@Valid @RequestBody GroomerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(groomerService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GroomerResponse> update(@PathVariable Long id,
                                                   @Valid @RequestBody GroomerRequest request) {
        return ResponseEntity.ok(groomerService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        groomerService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
