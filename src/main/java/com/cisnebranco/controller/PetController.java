package com.cisnebranco.controller;

import com.cisnebranco.dto.request.PetRequest;
import com.cisnebranco.dto.response.PetGroomerViewResponse;
import com.cisnebranco.dto.response.PetResponse;
import com.cisnebranco.security.UserPrincipal;
import com.cisnebranco.service.PetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/pets")
@RequiredArgsConstructor
@Tag(name = "Pets", description = "Pet management")
public class PetController {

    private final PetService petService;

    @Operation(summary = "List all pets with pagination")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<PetResponse>> findAll(@PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return ResponseEntity.ok(petService.findAll(pageable));
    }

    @Operation(summary = "List pets by client ID")
    @GetMapping("/by-client/{clientId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PetResponse>> findByClient(@PathVariable Long clientId) {
        return ResponseEntity.ok(petService.findByClient(clientId));
    }

    @Operation(summary = "Find a pet by ID")
    @GetMapping("/{id}")
    public ResponseEntity<?> findById(@PathVariable Long id,
                                       @AuthenticationPrincipal UserPrincipal principal) {
        if (principal.getRole() == com.cisnebranco.entity.enums.UserRole.GROOMER) {
            return ResponseEntity.ok(petService.findByIdGroomerView(id));
        }
        return ResponseEntity.ok(petService.findById(id));
    }

    @Operation(summary = "Create a new pet")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PetResponse> create(@Valid @RequestBody PetRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(petService.create(request));
    }

    @Operation(summary = "Update an existing pet")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PetResponse> update(@PathVariable Long id,
                                               @Valid @RequestBody PetRequest request) {
        return ResponseEntity.ok(petService.update(id, request));
    }
}
