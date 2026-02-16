package com.cisnebranco.security;

import com.cisnebranco.entity.enums.UserRole;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private static final String SECRET = "test-secret-key-that-is-long-enough-for-hs256-algorithm-min-256-bits";
    private static final long EXPIRATION_MS = 3600000; // 1h

    private JwtTokenProvider tokenProvider;

    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProvider(SECRET, EXPIRATION_MS);
    }

    @Test
    void generateAndValidate_success() {
        UserPrincipal principal = new UserPrincipal(
                1L, "admin", "pass", UserRole.ADMIN, null, true);

        String token = tokenProvider.generateAccessToken(principal);

        assertThat(tokenProvider.validateToken(token)).isTrue();
        assertThat(tokenProvider.getUsernameFromToken(token)).isEqualTo("admin");
        assertThat(tokenProvider.getTokenType(token)).isEqualTo("access");
    }

    @Test
    void generateToken_containsGroomerId() {
        UserPrincipal principal = new UserPrincipal(
                2L, "groomer", "pass", UserRole.GROOMER, 42L, true);

        String token = tokenProvider.generateAccessToken(principal);

        assertThat(tokenProvider.validateToken(token)).isTrue();
        assertThat(tokenProvider.getUsernameFromToken(token)).isEqualTo("groomer");
    }

    @Test
    void validateToken_expired_returnsFalse() {
        // Create provider with 0ms expiration — token is already expired
        JwtTokenProvider shortLived = new JwtTokenProvider(SECRET, 0);
        UserPrincipal principal = new UserPrincipal(
                1L, "admin", "pass", UserRole.ADMIN, null, true);

        String token = shortLived.generateAccessToken(principal);

        assertThat(tokenProvider.validateToken(token)).isFalse();
    }

    @Test
    void validateToken_wrongSignature_returnsFalse() {
        // Generate token with a different key
        SecretKey otherKey = Keys.hmacShaKeyFor(
                "other-secret-key-long-enough-for-hs256-algorithm-minimum-256-bits"
                        .getBytes(StandardCharsets.UTF_8));

        String token = Jwts.builder()
                .subject("admin")
                .claim("type", "access")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION_MS))
                .signWith(otherKey)
                .compact();

        assertThat(tokenProvider.validateToken(token)).isFalse();
    }

    @Test
    void validateToken_malformed_returnsFalse() {
        assertThat(tokenProvider.validateToken("not-a-jwt")).isFalse();
    }

    @Test
    void validateToken_null_returnsFalse() {
        assertThat(tokenProvider.validateToken(null)).isFalse();
    }
}
