package com.cisnebranco.controller;

import com.cisnebranco.dto.request.PetRequest;
import com.cisnebranco.dto.response.PetGroomerViewResponse;
import com.cisnebranco.dto.response.PetResponse;
import com.cisnebranco.security.UserPrincipal;
import com.cisnebranco.service.PetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/pets")
@RequiredArgsConstructor
public class PetController {

    private final PetService petService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PetResponse>> findAll() {
        return ResponseEntity.ok(petService.findAll());
    }

    @GetMapping("/by-client/{clientId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PetResponse>> findByClient(@PathVariable Long clientId) {
        return ResponseEntity.ok(petService.findByClient(clientId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> findById(@PathVariable Long id,
                                       @AuthenticationPrincipal UserPrincipal principal) {
        if (principal.getRole() == com.cisnebranco.entity.enums.UserRole.GROOMER) {
            return ResponseEntity.ok(petService.findByIdGroomerView(id));
        }
        return ResponseEntity.ok(petService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PetResponse> create(@Valid @RequestBody PetRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(petService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PetResponse> update(@PathVariable Long id,
                                               @Valid @RequestBody PetRequest request) {
        return ResponseEntity.ok(petService.update(id, request));
    }
}
