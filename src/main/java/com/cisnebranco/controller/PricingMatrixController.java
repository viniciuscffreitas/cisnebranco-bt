package com.cisnebranco.controller;

import com.cisnebranco.dto.request.PricingMatrixRequest;
import com.cisnebranco.dto.response.PricingMatrixResponse;
import com.cisnebranco.service.PricingMatrixService;
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
public class PricingMatrixController {

    private final PricingMatrixService pricingMatrixService;

    @GetMapping
    public ResponseEntity<List<PricingMatrixResponse>> findAll() {
        return ResponseEntity.ok(pricingMatrixService.findAll());
    }

    @PostMapping
    public ResponseEntity<PricingMatrixResponse> createOrUpdate(@Valid @RequestBody PricingMatrixRequest request) {
        return ResponseEntity.ok(pricingMatrixService.createOrUpdate(request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        pricingMatrixService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
