package com.cisnebranco.controller;

import com.cisnebranco.dto.request.PricingMatrixRequest;
import com.cisnebranco.dto.response.PricingMatrixResponse;
import com.cisnebranco.service.PricingMatrixService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/pricing")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Pricing Matrix", description = "Service pricing configuration per breed and size")
public class PricingMatrixController {

    private final PricingMatrixService pricingMatrixService;

    @Operation(summary = "List all pricing matrix entries")
    @GetMapping
    public ResponseEntity<List<PricingMatrixResponse>> findAll() {
        return ResponseEntity.ok(pricingMatrixService.findAll());
    }

    @Operation(summary = "Create or update a pricing matrix entry")
    @PostMapping
    public ResponseEntity<PricingMatrixResponse> createOrUpdate(@Valid @RequestBody PricingMatrixRequest request) {
        return ResponseEntity.ok(pricingMatrixService.createOrUpdate(request));
    }

    @Operation(summary = "Delete a pricing matrix entry by ID")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        pricingMatrixService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
