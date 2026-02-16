package com.cisnebranco.controller;

import com.cisnebranco.dto.request.CreateUserRequest;
import com.cisnebranco.dto.response.CurrentUserResponse;
import com.cisnebranco.dto.response.UserResponse;
import com.cisnebranco.security.UserPrincipal;
import com.cisnebranco.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User account management")
public class UserController {

    private final UserService userService;

    @Operation(summary = "Get current authenticated user info")
    @GetMapping("/me")
    public ResponseEntity<CurrentUserResponse> getCurrentUser(@AuthenticationPrincipal UserPrincipal principal) {
        Long groomerId = principal.getGroomerId();
        var response = new CurrentUserResponse(
                principal.getId(),
                principal.getUsername(),
                principal.getRole().name(),
                groomerId != null ? groomerId : 0
        );
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Create a new user account")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.create(request));
    }

    @Operation(summary = "List all users")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<UserResponse>> findAll() {
        return ResponseEntity.ok(userService.findAll());
    }

    @Operation(summary = "Deactivate a user account by ID")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        userService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
