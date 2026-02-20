package com.cisnebranco.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitFilterTest {

    private RateLimitFilter filter;

    @BeforeEach
    void setUp() {
        filter = new RateLimitFilter(60, 10, List.of("172.16.0.0/12"), new ObjectMapper());
    }

    // --- matchesCidr ---

    @Test
    void matchesCidr_exactMatch_returnsTrue() {
        assertThat(RateLimitFilter.matchesCidr("192.168.1.1", "192.168.1.1")).isTrue();
    }

    @Test
    void matchesCidr_ipInSubnet_returnsTrue() {
        assertThat(RateLimitFilter.matchesCidr("172.20.0.5", "172.16.0.0/12")).isTrue();
    }

    @Test
    void matchesCidr_ipOutsideSubnet_returnsFalse() {
        assertThat(RateLimitFilter.matchesCidr("192.168.1.1", "172.16.0.0/12")).isFalse();
    }

    @Test
    void matchesCidr_invalidIp_returnsFalse() {
        assertThat(RateLimitFilter.matchesCidr("not-an-ip", "172.16.0.0/12")).isFalse();
    }

    // --- getClientIp: direct connection (not trusted proxy) ---

    @Test
    void getClientIp_directConnection_usesRemoteAddr() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("203.0.113.42");
        request.addHeader("X-Forwarded-For", "1.2.3.4");

        // remoteAddr is not in trusted CIDR — X-Forwarded-For must be ignored
        assertThat(filter.getClientIp(request)).isEqualTo("203.0.113.42");
    }

    // --- getClientIp: trusted proxy (Nginx Proxy Manager in Docker) ---

    @Test
    void getClientIp_trustedProxy_usesFirstXForwardedForEntry() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("172.20.0.2"); // Docker NPM IP, inside 172.16.0.0/12
        request.addHeader("X-Forwarded-For", "203.0.113.42, 10.0.0.1");

        assertThat(filter.getClientIp(request)).isEqualTo("203.0.113.42");
    }

    @Test
    void getClientIp_trustedProxy_noXForwardedFor_fallsBackToRemoteAddr() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("172.20.0.2");

        assertThat(filter.getClientIp(request)).isEqualTo("172.20.0.2");
    }

    @Test
    void getClientIp_trustedProxy_emptyXForwardedFor_fallsBackToRemoteAddr() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("172.20.0.2");
        request.addHeader("X-Forwarded-For", "   ");

        assertThat(filter.getClientIp(request)).isEqualTo("172.20.0.2");
    }
}
