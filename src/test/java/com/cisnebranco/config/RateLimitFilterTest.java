package com.cisnebranco.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RateLimitFilterTest {

    private RateLimitFilter filter;

    @BeforeEach
    void setUp() {
        filter = new RateLimitFilter(60, 10, 5, 10, List.of("172.16.0.0/12"), new ObjectMapper());
    }

    // --- Constructor: startup CIDR validation ---

    @Test
    void constructor_invalidCidrPrefixLength_throwsIllegalArgument() {
        assertThatThrownBy(() ->
                new RateLimitFilter(60, 10, 5, 10, List.of("172.16.0.0/abc"), new ObjectMapper()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("172.16.0.0/abc");
    }

    @Test
    void constructor_invalidCidrHost_throwsIllegalArgument() {
        assertThatThrownBy(() ->
                new RateLimitFilter(60, 10, 5, 10, List.of("not-a-host/24"), new ObjectMapper()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not-a-host/24");
    }

    @Test
    void constructor_emptyCidrList_startsWithoutException() {
        // Valid scenario: no proxy (e.g. direct deployment without Nginx)
        RateLimitFilter f = new RateLimitFilter(60, 10, 5, 10, List.of(), new ObjectMapper());
        assertThat(f).isNotNull();
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

    @Test
    void matchesCidr_invalidCidrPrefixLength_returnsFalse() {
        assertThat(RateLimitFilter.matchesCidr("172.20.0.5", "172.16.0.0/abc")).isFalse();
    }

    @Test
    void matchesCidr_invalidCidrHost_returnsFalse() {
        assertThat(RateLimitFilter.matchesCidr("172.20.0.5", "not-a-host/24")).isFalse();
    }

    @Test
    void matchesCidr_zeroPrefixLength_matchesAllIps() {
        // /0 = match every address — document this semantic so regressions in the loop
        // condition are immediately visible
        assertThat(RateLimitFilter.matchesCidr("1.2.3.4", "0.0.0.0/0")).isTrue();
        assertThat(RateLimitFilter.matchesCidr("203.0.113.5", "0.0.0.0/0")).isTrue();
    }

    @Test
    void matchesCidr_networkBoundaryAddress_returnsTrue() {
        assertThat(RateLimitFilter.matchesCidr("172.16.0.0", "172.16.0.0/12")).isTrue();
    }

    @Test
    void matchesCidr_broadcastBoundaryAddress_returnsTrue() {
        assertThat(RateLimitFilter.matchesCidr("172.31.255.255", "172.16.0.0/12")).isTrue();
    }

    @Test
    void matchesCidr_oneAddressBeyondSubnetBoundary_returnsFalse() {
        assertThat(RateLimitFilter.matchesCidr("172.32.0.1", "172.16.0.0/12")).isFalse();
    }

    @Test
    void matchesCidr_ipv4MappedIpv6VsIpv4Cidr_returnsTrue() {
        // Java's InetAddress.getByName("::ffff:172.20.0.2") unwraps the IPv4-mapped
        // address to a 4-byte Inet4Address, so it DOES match IPv4 CIDRs.
        // This is actually the desired behavior: Docker remoteAddr in dual-stack
        // environments is correctly matched against the configured IPv4 CIDR.
        assertThat(RateLimitFilter.matchesCidr("::ffff:172.20.0.2", "172.16.0.0/12")).isTrue();
    }

    // --- isNumericIpAddress ---

    @Test
    void isNumericIpAddress_validIpv4_returnsTrue() {
        assertThat(RateLimitFilter.isNumericIpAddress("203.0.113.42")).isTrue();
    }

    @Test
    void isNumericIpAddress_validIpv6_returnsTrue() {
        assertThat(RateLimitFilter.isNumericIpAddress("::1")).isTrue();
        assertThat(RateLimitFilter.isNumericIpAddress("fe80::1")).isTrue();
        // Note: scope IDs like "fe80::1%eth0" contain non-hex chars (t, h) and are
        // not accepted by the current regex — this is acceptable since scope IDs
        // only appear on direct loopback/link-local connections, never through NPM.
    }

    @Test
    void isNumericIpAddress_hostname_returnsFalse() {
        assertThat(RateLimitFilter.isNumericIpAddress("evil.example.com")).isFalse();
    }

    @Test
    void isNumericIpAddress_blank_returnsFalse() {
        assertThat(RateLimitFilter.isNumericIpAddress("   ")).isFalse();
        assertThat(RateLimitFilter.isNumericIpAddress("")).isFalse();
    }

    // --- getClientIp: direct connection (not a trusted proxy) ---

    @Test
    void getClientIp_directConnection_usesRemoteAddr() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("203.0.113.42");
        request.addHeader("X-Forwarded-For", "1.2.3.4");

        // remoteAddr is not in trusted CIDR — X-Forwarded-For must be ignored
        assertThat(filter.getClientIp(request)).isEqualTo("203.0.113.42");
    }

    // --- getClientIp: trusted proxy ---

    @Test
    void getClientIp_trustedProxy_usesFirstXForwardedForEntry() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("172.20.0.2"); // inside 172.16.0.0/12
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

    @Test
    void getClientIp_trustedProxy_commaLeadingXForwardedFor_fallsBackToRemoteAddr() {
        // First token is blank after trim — inner guard fires, fallback to remoteAddr
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("172.20.0.2");
        request.addHeader("X-Forwarded-For", ", 203.0.113.42");

        assertThat(filter.getClientIp(request)).isEqualTo("172.20.0.2");
    }

    @Test
    void getClientIp_trustedProxy_xForwardedForWithLeadingSpaceOnFirstEntry_trimsCorrectly() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("172.20.0.2");
        request.addHeader("X-Forwarded-For", "  203.0.113.42  , 10.0.0.1");

        assertThat(filter.getClientIp(request)).isEqualTo("203.0.113.42");
    }

    @Test
    void getClientIp_trustedProxy_nonNumericXForwardedFor_fallsBackToRemoteAddr() {
        // Hostname injection should be rejected (avoids DNS lookup on hot path)
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("172.20.0.2");
        request.addHeader("X-Forwarded-For", "evil.example.com");

        assertThat(filter.getClientIp(request)).isEqualTo("172.20.0.2");
    }

    @Test
    void getClientIp_multipleTrustedCidrs_matchesAny() {
        RateLimitFilter multiFilter = new RateLimitFilter(
                60, 10, 5, 10, List.of("10.0.0.0/8", "192.168.0.0/16"), new ObjectMapper());

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.1.50"); // in second CIDR
        request.addHeader("X-Forwarded-For", "203.0.113.1");

        assertThat(multiFilter.getClientIp(request)).isEqualTo("203.0.113.1");
    }

    @Test
    void getClientIp_emptyTrustedCidrList_alwaysUsesRemoteAddr() {
        RateLimitFilter noProxyFilter = new RateLimitFilter(60, 10, 5, 10, List.of(), new ObjectMapper());

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("172.20.0.2");
        request.addHeader("X-Forwarded-For", "203.0.113.42");

        assertThat(noProxyFilter.getClientIp(request)).isEqualTo("172.20.0.2");
    }

    // --- resolveCategory ---

    @Test
    void resolveCategory_authPath_returnsAuth() {
        assertThat(filter.resolveCategory("/auth/login").name()).isEqualTo("auth");
        assertThat(filter.resolveCategory("/auth/refresh").name()).isEqualTo("auth");
    }

    @Test
    void resolveCategory_photoUpload_returnsUpload() {
        assertThat(filter.resolveCategory("/os/123/photos").name()).isEqualTo("upload");
        assertThat(filter.resolveCategory("/os/456/photos/upload").name()).isEqualTo("upload");
    }

    @Test
    void resolveCategory_checklist_returnsUpload() {
        assertThat(filter.resolveCategory("/os/123/checklist").name()).isEqualTo("upload");
    }

    @Test
    void resolveCategory_reports_returnsReport() {
        assertThat(filter.resolveCategory("/reports").name()).isEqualTo("report");
        assertThat(filter.resolveCategory("/reports/daily").name()).isEqualTo("report");
    }

    @Test
    void resolveCategory_regularEndpoint_returnsGeneral() {
        assertThat(filter.resolveCategory("/clients").name()).isEqualTo("general");
        assertThat(filter.resolveCategory("/os").name()).isEqualTo("general");
        assertThat(filter.resolveCategory("/pets/123").name()).isEqualTo("general");
    }

    @Test
    void resolveCategory_limitsMatchConfiguration() {
        assertThat(filter.resolveCategory("/auth/login").limit()).isEqualTo(10);
        assertThat(filter.resolveCategory("/os/1/photos").limit()).isEqualTo(5);
        assertThat(filter.resolveCategory("/reports/daily").limit()).isEqualTo(10);
        assertThat(filter.resolveCategory("/clients").limit()).isEqualTo(60);
    }

    @Test
    void resolveCategory_nonNumericOsId_returnsGeneral() {
        assertThat(filter.resolveCategory("/os/abc/photos").name()).isEqualTo("general");
        assertThat(filter.resolveCategory("/os/abc/checklist").name()).isEqualTo("general");
    }

    @Test
    void resolveCategory_authWithoutTrailingSlash_returnsGeneral() {
        assertThat(filter.resolveCategory("/auth").name()).isEqualTo("general");
    }

    @Test
    void resolveCategory_reportsPrefixWithoutSlash_returnsGeneral() {
        // /reports-admin should NOT match the report category
        assertThat(filter.resolveCategory("/reports-admin").name()).isEqualTo("general");
    }

    @Test
    void resolveCategory_checklistSubpath_returnsUpload() {
        assertThat(filter.resolveCategory("/os/123/checklist/items").name()).isEqualTo("upload");
    }

    // --- Integration: separate buckets per category ---

    @Test
    void doFilter_sameIp_differentCategories_useSeparateBuckets() throws Exception {
        RateLimitFilter testFilter = new RateLimitFilter(
                2, 2, 2, 2, List.of(), new ObjectMapper());
        FilterChain chain = new MockFilterChain();

        // Exhaust auth bucket (2 requests)
        for (int i = 0; i < 2; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/auth/login");
            req.setServletPath("/auth/login");
            req.setRemoteAddr("1.2.3.4");
            testFilter.doFilterInternal(req, new MockHttpServletResponse(), chain);
        }

        // Auth should be exhausted
        MockHttpServletRequest authReq = new MockHttpServletRequest("POST", "/auth/login");
        authReq.setServletPath("/auth/login");
        authReq.setRemoteAddr("1.2.3.4");
        MockHttpServletResponse authResp = new MockHttpServletResponse();
        testFilter.doFilterInternal(authReq, authResp, chain);
        assertThat(authResp.getStatus()).isEqualTo(429);

        // General bucket should still be available for same IP
        MockHttpServletRequest generalReq = new MockHttpServletRequest("GET", "/clients");
        generalReq.setServletPath("/clients");
        generalReq.setRemoteAddr("1.2.3.4");
        MockHttpServletResponse generalResp = new MockHttpServletResponse();
        testFilter.doFilterInternal(generalReq, generalResp, chain);
        assertThat(generalResp.getStatus()).isEqualTo(200);
    }
}
