package com.eric6166.taskapi.controller;

import com.eric6166.taskapi.dto.auth.AuthResponse;
import com.eric6166.taskapi.dto.auth.RegisterResponse;
import com.eric6166.taskapi.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @Test
    void register_validRequest_returns201() throws Exception {
        RegisterResponse response = new RegisterResponse(1L, "user@test.com", OffsetDateTime.now());
        when(authService.register(any())).thenReturn(response);

        String body = """
                {"username":"user@test.com","password":"password123"}
                """;

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("user@test.com"));
    }

    @Test
    void register_invalidEmail_returns400() throws Exception {
        String body = """
                {"username":"not-an-email","password":"password123"}
                """;

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_validRequest_returns200() throws Exception {
        AuthResponse response = new AuthResponse("jwt.token.value", 86400000L);
        when(authService.login(any())).thenReturn(response);

        String body = """
                {"username":"user@test.com","password":"password123"}
                """;

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt.token.value"))
                .andExpect(jsonPath("$.expiresIn").value(86400000));
    }
}
