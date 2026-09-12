package com.aml.system.controller;

import com.aml.system.dto.admin.ComplianceOfficerCreateDto;
import com.aml.system.dto.admin.ComplianceOfficerResponseDto;
import com.aml.system.service.ComplianceOfficerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ComplianceOfficerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ComplianceOfficerService officerService;

    @Test
    @DisplayName("POST /compliance-officers: Tenant Admin creates a new compliance officer")
    @WithMockUser(roles = "TENANT_ADMIN")
    void createOfficer_success() throws Exception {
        ComplianceOfficerCreateDto dto = new ComplianceOfficerCreateDto();
        dto.setFullName("John Doe");
        dto.setUsername("johndoe");
        dto.setEmail("john.doe@bank.com");

        ComplianceOfficerResponseDto response = ComplianceOfficerResponseDto.builder()
                .userId(UUID.randomUUID())
                .fullName("John Doe")
                .username("johndoe")
                .email("john.doe@bank.com")
                .isActive(true)
                .build();

        when(officerService.createComplianceOfficer(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/bank-admin/compliance-officers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("johndoe"));
    }

    @Test
    @DisplayName("POST /compliance-officers: Invalid email rejected with 400 Bad Request")
    @WithMockUser(roles = "TENANT_ADMIN")
    void createOfficer_invalidEmail_badRequest() throws Exception {
        ComplianceOfficerCreateDto dto = new ComplianceOfficerCreateDto();
        dto.setFullName("John Doe");
        dto.setUsername("johndoe");
        dto.setEmail("invalid-email-no-domain");

        mockMvc.perform(post("/api/v1/bank-admin/compliance-officers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /compliance-officers: Lists officers for Tenant Admin")
    @WithMockUser(roles = "TENANT_ADMIN")
    void getOfficers_tenantAdmin_success() throws Exception {
        when(officerService.getComplianceOfficers()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/bank-admin/compliance-officers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("PUT /compliance-officers/{id}/deactivate: Deactivates officer")
    @WithMockUser(roles = "TENANT_ADMIN")
    void deactivateOfficer_success() throws Exception {
        UUID officerId = UUID.randomUUID();
        when(officerService.deactivateComplianceOfficer(officerId)).thenReturn("Officer deactivated successfully");

        mockMvc.perform(put("/api/v1/bank-admin/compliance-officers/" + officerId + "/deactivate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Officer deactivated successfully"));
    }

    @Test
    @DisplayName("PUT /compliance-officers/{id}/reactivate: Reactivates officer")
    @WithMockUser(roles = "TENANT_ADMIN")
    void reactivateOfficer_success() throws Exception {
        UUID officerId = UUID.randomUUID();
        when(officerService.reactivateComplianceOfficer(officerId)).thenReturn("Officer reactivated successfully");

        mockMvc.perform(put("/api/v1/bank-admin/compliance-officers/" + officerId + "/reactivate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Officer reactivated successfully"));
    }

    @Test
    @DisplayName("GET /compliance-officers: Non-admin role is forbidden (403)")
    @WithMockUser(roles = "COMPLIANCE_OFFICER")
    void getOfficers_forbiddenForOfficers() throws Exception {
        mockMvc.perform(get("/api/v1/bank-admin/compliance-officers"))
                .andExpect(status().isForbidden());
    }
}
