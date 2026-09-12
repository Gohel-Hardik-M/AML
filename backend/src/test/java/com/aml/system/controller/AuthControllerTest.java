package com.aml.system.controller;

import com.aml.system.dto.auth.LoginRequestDto;
import com.aml.system.dto.auth.LoginResponseDto;
import com.aml.system.dto.auth.MasterLoginRequestDto;
import com.aml.system.dto.auth.PasswordResetDto;
import com.aml.system.exception.AmlBusinessException;
import com.aml.system.multitenancy.TenantRoutingDataSource;
import com.aml.system.service.AuthService;
import com.aml.system.service.MasterAuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private MasterAuthService masterAuthService;

    @Autowired
    private TenantRoutingDataSource routingDataSource;

    @org.junit.jupiter.api.BeforeEach
    void setupTenant() {
        if (!routingDataSource.hasTenant("BANK_A")) {
            routingDataSource.addTenantDataSource("BANK_A", "jdbc:h2:mem:aml_test", "sa", "");
        }
    }

    @Test
    @DisplayName("POST /api/v1/auth/login: Successful login returns token and user info")
    void login_success() throws Exception {
        LoginRequestDto request = new LoginRequestDto();
        request.setUsername("officer_bob");
        request.setPassword("Password@123");
        request.setTenantId("BANK_A");

        LoginResponseDto responseDto = LoginResponseDto.builder()
                .token("mock-jwt-token")
                .isTemporaryPassword(false)
                .message("Login successful")
                .build();

        when(authService.login(any(LoginRequestDto.class), any())).thenReturn(responseDto);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").value("mock-jwt-token"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login: Rejects login when tenant is not active or registered")
    void login_unknownTenant_returnsBadRequest() throws Exception {
        LoginRequestDto request = new LoginRequestDto();
        request.setUsername("admin");
        request.setPassword("Password@123");
        request.setTenantId("UNKNOWN_BANK");
        // routingDataSource is a real bean and naturally returns false for UNKNOWN_BANK

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login: Blank username or password fails validation (400)")
    void login_blankFields_failsValidation() throws Exception {
        LoginRequestDto request = new LoginRequestDto();
        request.setUsername("");
        request.setPassword("");
        request.setTenantId("BANK_A");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/auth/master/login: Master admin login succeeds")
    void masterLogin_success() throws Exception {
        MasterLoginRequestDto request = new MasterLoginRequestDto();
        request.setUsername("superadmin");
        request.setPassword("SuperPass!2026");

        LoginResponseDto responseDto = LoginResponseDto.builder()
                .token("master-token")
                .isTemporaryPassword(false)
                .message("Master login successful")
                .build();

        when(masterAuthService.masterLogin(any(), any())).thenReturn(responseDto);

        mockMvc.perform(post("/api/v1/auth/master/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").value("master-token"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/reset-password: Unauthenticated call is rejected (401/403)")
    void resetPassword_unauthenticated_rejected() throws Exception {
        PasswordResetDto resetDto = new PasswordResetDto();
        resetDto.setCurrentPassword("OldPass@123");
        resetDto.setNewPassword("NewPass@2026#Valid");

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetDto)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/v1/auth/reset-password: Password complexity failure rejected (400)")
    @WithMockUser(username = "admin", roles = {"TENANT_ADMIN"})
    void resetPassword_weakPassword_failsValidation() throws Exception {
        PasswordResetDto resetDto = new PasswordResetDto();
        resetDto.setCurrentPassword("OldPass@123");
        resetDto.setNewPassword("weak"); // Too short, missing upper/digit/symbol

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetDto)))
                .andExpect(status().isBadRequest());
    }
}
