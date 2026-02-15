package com.cisnebranco.service;

import com.cisnebranco.dto.request.LoginRequest;
import com.cisnebranco.dto.request.RefreshTokenRequest;
import com.cisnebranco.dto.response.AuthResponse;
import com.cisnebranco.exception.BusinessException;
import com.cisnebranco.security.CustomUserDetailsService;
import com.cisnebranco.security.JwtTokenProvider;
import com.cisnebranco.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final CustomUserDetailsService userDetailsService;

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        return buildAuthResponse(principal);
    }

    public AuthResponse refresh(RefreshTokenRequest request) {
        String refreshToken = request.refreshToken();

        if (!tokenProvider.validateToken(refreshToken)
                || !"refresh".equals(tokenProvider.getTokenType(refreshToken))) {
            throw new BusinessException("Invalid refresh token");
        }

        String username = tokenProvider.getUsernameFromToken(refreshToken);
        UserPrincipal principal = (UserPrincipal) userDetailsService.loadUserByUsername(username);

        return buildAuthResponse(principal);
    }

    private AuthResponse buildAuthResponse(UserPrincipal principal) {
        return new AuthResponse(
                tokenProvider.generateAccessToken(principal),
                tokenProvider.generateRefreshToken(principal),
                principal.getRole().name(),
                principal.getGroomerId()
        );
    }
}
