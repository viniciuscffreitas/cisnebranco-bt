package com.cisnebranco.controller;

import com.cisnebranco.BaseIntegrationTest;
import com.cisnebranco.entity.AppUser;
import com.cisnebranco.entity.Groomer;
import com.cisnebranco.entity.enums.UserRole;
import com.cisnebranco.repository.AppUserRepository;
import com.cisnebranco.repository.GroomerRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SecurityIntegrationTest extends BaseIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private AppUserRepository userRepository;
    @Autowired private GroomerRepository groomerRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private String adminToken;
    private String groomerToken;

    @BeforeEach
    void setUp() throws Exception {
        // Create admin user
        AppUser admin = new AppUser();
        admin.setUsername("secadmin");
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setRole(UserRole.ADMIN);
        admin.setActive(true);
        userRepository.save(admin);

        // Create groomer + groomer user
        Groomer groomer = new Groomer();
        groomer.setName("Sec Groomer");
        groomer.setPhone("11999999999");
        groomerRepository.save(groomer);

        AppUser groomerUser = new AppUser();
        groomerUser.setUsername("secgroomer");
        groomerUser.setPassword(passwordEncoder.encode("groomer123"));
        groomerUser.setRole(UserRole.GROOMER);
        groomerUser.setGroomer(groomer);
        groomerUser.setActive(true);
        userRepository.save(groomerUser);

        adminToken = login("secadmin", "admin123");
        groomerToken = login("secgroomer", "groomer123");
    }

    // --- Public endpoints ---

    @Test
    void healthEndpoint_noAuth_returns200() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void authLogin_noAuth_isAccessible() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"username": "secadmin", "password": "admin123"}
                        """))
                .andExpect(status().isOk());
    }

    // --- Protected endpoints without auth ---

    @Test
    void clientsEndpoint_noAuth_isForbidden() throws Exception {
        mockMvc.perform(get("/clients"))
                .andExpect(status().isForbidden());
    }

    @Test
    void osEndpoint_noAuth_isForbidden() throws Exception {
        mockMvc.perform(get("/os"))
                .andExpect(status().isForbidden());
    }

    @Test
    void breedsEndpoint_noAuth_isForbidden() throws Exception {
        mockMvc.perform(get("/breeds"))
                .andExpect(status().isForbidden());
    }

    // --- Role-based access ---

    @Test
    void clientsEndpoint_adminRole_returns200() throws Exception {
        mockMvc.perform(get("/clients")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void clientsEndpoint_groomerRole_returns403() throws Exception {
        mockMvc.perform(get("/clients")
                        .header("Authorization", "Bearer " + groomerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void osEndpoint_adminRole_returns200() throws Exception {
        mockMvc.perform(get("/os")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void osEndpoint_groomerRole_returns200() throws Exception {
        mockMvc.perform(get("/os")
                        .header("Authorization", "Bearer " + groomerToken))
                .andExpect(status().isOk());
    }

    @Test
    void breedsEndpoint_authenticated_returns200() throws Exception {
        mockMvc.perform(get("/breeds")
                        .header("Authorization", "Bearer " + groomerToken))
                .andExpect(status().isOk());
    }

    @Test
    void serviceTypesEndpoint_authenticated_returns200() throws Exception {
        mockMvc.perform(get("/service-types")
                        .header("Authorization", "Bearer " + groomerToken))
                .andExpect(status().isOk());
    }

    // --- Invalid token ---

    @Test
    void protectedEndpoint_invalidToken_isForbidden() throws Exception {
        mockMvc.perform(get("/clients")
                        .header("Authorization", "Bearer invalid-token-here"))
                .andExpect(status().isForbidden());
    }

    @Test
    void protectedEndpoint_malformedToken_isForbidden() throws Exception {
        mockMvc.perform(get("/os")
                        .header("Authorization", "Bearer eyJhbGciOiJIUzI1NiJ9.expired.sig"))
                .andExpect(status().isForbidden());
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
