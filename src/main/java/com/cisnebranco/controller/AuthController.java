package com.cisnebranco.controller;

import com.cisnebranco.dto.request.LoginRequest;
import com.cisnebranco.dto.request.RefreshTokenRequest;
import com.cisnebranco.dto.response.AuthResponse;
import com.cisnebranco.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Login, token refresh and logout")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Login with username and password", description = "Returns user info and sets JWT cookies")
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                               HttpServletResponse response) {
        return ResponseEntity.ok(authService.login(request, response));
    }

    @Operation(summary = "Refresh access token", description = "Exchange a valid refresh token for a new token pair")
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request,
                                                 HttpServletResponse response) {
        return ResponseEntity.ok(authService.refresh(request, response));
    }

    @Operation(summary = "Logout", description = "Revokes all refresh tokens and clears cookies")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody(required = false) RefreshTokenRequest request,
                                        HttpServletResponse response) {
        authService.logout(request != null ? request.refreshToken() : null, response);
        return ResponseEntity.noContent().build();
    }
}
