package com.cisnebranco.service;

import com.cisnebranco.dto.request.LoginRequest;
import com.cisnebranco.dto.request.RefreshTokenRequest;
import com.cisnebranco.dto.response.AuthResponse;
import com.cisnebranco.entity.AppUser;
import com.cisnebranco.entity.RefreshToken;
import com.cisnebranco.exception.BusinessException;
import com.cisnebranco.exception.UnauthorizedException;
import com.cisnebranco.repository.AppUserRepository;
import com.cisnebranco.repository.RefreshTokenRepository;
import com.cisnebranco.security.CustomUserDetailsService;
import com.cisnebranco.security.JwtTokenProvider;
import com.cisnebranco.security.UserPrincipal;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final CustomUserDetailsService userDetailsService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AppUserRepository appUserRepository;
    private final AuditService auditService;

    @Value("${app.jwt.refresh-token-expiration-ms}")
    private long refreshTokenExpirationMs;

    @Value("${app.jwt.access-token-expiration-ms}")
    private long accessTokenExpirationMs;

    @Transactional
    public AuthResponse login(LoginRequest request, HttpServletResponse response) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        String accessToken = tokenProvider.generateAccessToken(principal);
        String refreshToken = createRefreshToken(principal.getId());

        setAuthCookies(response, accessToken, refreshToken);

        auditService.log("LOGIN", "AppUser", principal.getId(), "User logged in", principal.getUsername());

        return new AuthResponse(accessToken, refreshToken, principal.getRole().name(),
                principal.getGroomerId(), principal.getUsername());
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request, HttpServletResponse response) {
        RefreshToken stored = refreshTokenRepository.findByToken(request.refreshToken())
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (stored.isExpired()) {
            refreshTokenRepository.delete(stored);
            throw new UnauthorizedException("Refresh token expired");
        }

        refreshTokenRepository.delete(stored);

        UserPrincipal principal = (UserPrincipal) userDetailsService
                .loadUserByUsername(stored.getUser().getUsername());
        String accessToken = tokenProvider.generateAccessToken(principal);
        String newRefreshToken = createRefreshToken(principal.getId());

        setAuthCookies(response, accessToken, newRefreshToken);

        return new AuthResponse(accessToken, newRefreshToken, principal.getRole().name(),
                principal.getGroomerId(), principal.getUsername());
    }

    @Transactional
    public void logout(String refreshToken, HttpServletResponse response) {
        if (refreshToken != null) {
            refreshTokenRepository.findByToken(refreshToken)
                    .ifPresentOrElse(
                            token -> {
                                Long userId = token.getUser().getId();
                                refreshTokenRepository.deleteByUserId(userId);
                                log.info("User {} logged out, all refresh tokens revoked", userId);
                            },
                            () -> log.warn("Logout attempted with unknown refresh token")
                    );
        }

        clearAuthCookies(response);
    }

    private void setAuthCookies(HttpServletResponse response, String accessToken, String refreshToken) {
        ResponseCookie jwtCookie = ResponseCookie.from("jwt", accessToken)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(Duration.ofMillis(accessTokenExpirationMs))
                .sameSite("Lax")
                .build();

        ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", refreshToken)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(Duration.ofMillis(refreshTokenExpirationMs))
                .sameSite("Lax")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, jwtCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
    }

    private void clearAuthCookies(HttpServletResponse response) {
        ResponseCookie jwtCookie = ResponseCookie.from("jwt", "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();

        ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, jwtCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
    }

    private String createRefreshToken(Long userId) {
        refreshTokenRepository.deleteExpired(LocalDateTime.now());

        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("User not found"));

        RefreshToken rt = new RefreshToken();
        rt.setToken(UUID.randomUUID().toString());
        rt.setUser(user);
        rt.setExpiresAt(LocalDateTime.now().plusSeconds(refreshTokenExpirationMs / 1000));
        refreshTokenRepository.save(rt);

        return rt.getToken();
    }
}
