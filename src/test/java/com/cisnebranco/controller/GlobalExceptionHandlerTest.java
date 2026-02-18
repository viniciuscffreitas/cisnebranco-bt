package com.cisnebranco.controller;

import com.cisnebranco.BaseIntegrationTest;
import com.cisnebranco.entity.AppUser;
import com.cisnebranco.entity.enums.UserRole;
import com.cisnebranco.repository.AppUserRepository;
import com.cisnebranco.repository.RefreshTokenRepository;
import com.cisnebranco.service.TechnicalOsService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest extends BaseIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private AppUserRepository userRepository;
    @Autowired private RefreshTokenRepository refreshTokenRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    // osService.enforceAccess is void — Mockito does nothing by default, allowing the
    // request to reach adjustServiceItemPrice where the exception is thrown.
    @MockitoBean private TechnicalOsService osService;

    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        AppUser admin = new AppUser();
        admin.setUsername("handler-test-admin");
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setRole(UserRole.ADMIN);
        admin.setActive(true);
        userRepository.save(admin);
        adminToken = login("handler-test-admin", "admin123");
    }

    @AfterEach
    void tearDown() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void adjustPrice_pessimisticLockContention_returns409() throws Exception {
        assertLockExceptionReturns409(
                new PessimisticLockingFailureException("lock contention"),
                "Este registro está sendo alterado por outro usuário. Aguarde um momento e tente novamente.");
    }

    @Test
    void adjustPrice_cannotAcquireLock_returns409() throws Exception {
        // CannotAcquireLockException is the concrete subclass thrown by Hibernate under real contention
        assertLockExceptionReturns409(
                new CannotAcquireLockException("cannot acquire lock"),
                "Este registro está sendo alterado por outro usuário. Aguarde um momento e tente novamente.");
    }

    @Test
    void adjustPrice_lockTimeout_returns409() throws Exception {
        assertLockExceptionReturns409(
                new QueryTimeoutException("lock timeout"),
                "Não foi possível obter acesso exclusivo ao registro. Aguarde um momento e tente novamente.");
    }

    private void assertLockExceptionReturns409(RuntimeException exception, String expectedMessage) throws Exception {
        when(osService.adjustServiceItemPrice(anyLong(), anyLong(), any()))
                .thenThrow(exception);

        mockMvc.perform(patch("/os/1/services/1/price")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"adjustedPrice": 60.00}
                        """))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value(expectedMessage))
                .andExpect(jsonPath("$.details").isArray())
                .andExpect(jsonPath("$.details").isEmpty());
    }

    private String login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"username": "%s", "password": "%s"}
                        """.formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("accessToken").asText();
    }
}
