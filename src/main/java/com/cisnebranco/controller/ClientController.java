package com.cisnebranco.controller;

import com.cisnebranco.dto.request.ClientFilterRequest;
import com.cisnebranco.dto.request.ClientRequest;
import com.cisnebranco.dto.response.ClientResponse;
import com.cisnebranco.service.ClientService;
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
public class ClientController {

    private final ClientService clientService;

    @GetMapping
    public ResponseEntity<Page<ClientResponse>> findAll(@PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return ResponseEntity.ok(clientService.findAll(pageable));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<ClientResponse>> search(@RequestParam(required = false) String name,
                                                        @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return ResponseEntity.ok(clientService.search(name, pageable));
    }

    @GetMapping("/filter")
    public ResponseEntity<Page<ClientResponse>> filter(@ModelAttribute ClientFilterRequest filter,
                                                        @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return ResponseEntity.ok(clientService.findByFilters(filter, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClientResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(clientService.findById(id));
    }

    @PostMapping
    public ResponseEntity<ClientResponse> create(@Valid @RequestBody ClientRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clientService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClientResponse> update(@PathVariable Long id,
                                                  @Valid @RequestBody ClientRequest request) {
        return ResponseEntity.ok(clientService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        clientService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
