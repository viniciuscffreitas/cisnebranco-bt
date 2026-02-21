package com.cisnebranco.controller;

import com.cisnebranco.dto.request.ClientFilterRequest;
import com.cisnebranco.dto.request.ClientRequest;
import com.cisnebranco.dto.response.ClientResponse;
import com.cisnebranco.service.ClientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/clients")
@RequiredArgsConstructor
@Tag(name = "Clients", description = "Client management")
public class ClientController {

    private final ClientService clientService;

    @Operation(summary = "List all clients with pagination")
    @GetMapping
    public ResponseEntity<Page<ClientResponse>> findAll(@PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return ResponseEntity.ok(clientService.findAll(pageable));
    }

    @Operation(summary = "Search clients by name, CPF, or phone")
    @GetMapping("/search")
    public ResponseEntity<Page<ClientResponse>> search(@RequestParam(required = false) String name,
                                                        @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return ResponseEntity.ok(clientService.search(name, pageable));
    }

    @Operation(summary = "Filter clients by multiple criteria")
    @GetMapping("/filter")
    public ResponseEntity<Page<ClientResponse>> filter(@ModelAttribute ClientFilterRequest filter,
                                                        @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return ResponseEntity.ok(clientService.findByFilters(filter, pageable));
    }

    @Operation(summary = "Find a client by ID")
    @GetMapping("/{id}")
    public ResponseEntity<ClientResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(clientService.findById(id));
    }

    @Operation(summary = "Create a new client")
    @PostMapping
    public ResponseEntity<ClientResponse> create(@Valid @RequestBody ClientRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clientService.create(request));
    }

    @Operation(summary = "Update an existing client")
    @PutMapping("/{id}")
    public ResponseEntity<ClientResponse> update(@PathVariable Long id,
                                                  @Valid @RequestBody ClientRequest request) {
        return ResponseEntity.ok(clientService.update(id, request));
    }

    @Operation(summary = "Delete a client by ID")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        clientService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
