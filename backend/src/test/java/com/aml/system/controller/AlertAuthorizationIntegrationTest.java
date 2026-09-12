package com.aml.system.controller;

import com.aml.system.repository.AlertRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AlertAuthorizationIntegrationTest {
    @Autowired MockMvc mockMvc;
    @MockBean AlertRepository alertRepository;

    @Test
    @WithMockUser(roles = "TENANT_ADMIN")
    void tenantAdminCanReadAdministrativeAlerts() throws Exception {
        when(alertRepository.findAll(any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/v1/alerts"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "COMPLIANCE_OFFICER")
    void complianceOfficerCannotReadAdministrativeAlerts() throws Exception {
        mockMvc.perform(get("/api/v1/alerts"))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedUserCannotReadAdministrativeAlerts() throws Exception {
        mockMvc.perform(get("/api/v1/alerts"))
                .andExpect(status().isUnauthorized());
    }
}
