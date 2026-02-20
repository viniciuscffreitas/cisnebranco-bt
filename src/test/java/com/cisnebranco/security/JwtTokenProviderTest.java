package com.cisnebranco.security;

import com.cisnebranco.entity.enums.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
    void generateToken_containsRoleAndGroomerId() {
        UserPrincipal principal = new UserPrincipal(
                2L, "groomer", "pass", UserRole.GROOMER, 42L, true);

        String token = tokenProvider.generateAccessToken(principal);

        assertThat(tokenProvider.validateToken(token)).isTrue();
        assertThat(tokenProvider.getUsernameFromToken(token)).isEqualTo("groomer");

        // Verify claims contain role and groomerId
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        Claims claims = Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token).getPayload();
        assertThat(claims.get("role", String.class)).isEqualTo("GROOMER");
        assertThat(claims.get("groomerId", Long.class)).isEqualTo(42L);
    }

    @Test
    void validateToken_expired_returnsFalse() {
        // Build an already-expired token directly
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        String expiredToken = Jwts.builder()
                .subject("admin")
                .claim("type", "access")
                .issuedAt(new Date(System.currentTimeMillis() - 7200000))
                .expiration(new Date(System.currentTimeMillis() - 3600000))
                .signWith(key)
                .compact();

        assertThat(tokenProvider.validateToken(expiredToken)).isFalse();
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

    @Test
    void constructor_shortSecret_throwsIllegalState() {
        assertThatThrownBy(() -> new JwtTokenProvider("too-short", EXPIRATION_MS))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 characters");
    }

    @Test
    void constructor_secretExactly32Chars_succeeds() {
        // 32 chars = minimum 256-bit key — must not throw
        String minSecret = "a".repeat(32);
        assertThat(new JwtTokenProvider(minSecret, EXPIRATION_MS)).isNotNull();
    }
}
