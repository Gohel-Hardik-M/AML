package com.aml.system.controller;

import com.aml.system.dto.admin.TenantOnboardRequestDto;
import com.aml.system.dto.admin.TenantSummaryDto;
import com.aml.system.exception.AmlBusinessException;
import com.aml.system.service.TenantProvisioningService;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TenantProvisioningControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TenantProvisioningService tenantProvisioningService;

    @Test
    @DisplayName("POST /master/tenants: System Admin successfully provisions a new bank tenant")
    @WithMockUser(roles = "SYSTEM_ADMIN")
    void onboardBank_systemAdmin_success() throws Exception {
        TenantOnboardRequestDto dto = new TenantOnboardRequestDto();
        dto.setTenantCode("HDFC_BANK");
        dto.setBankName("HDFC Bank Ltd");
        dto.setAdminUsername("admin_hdfc");
        dto.setAdminEmail("admin@hdfc.com");

        when(tenantProvisioningService.onboardNewBank(any()))
                .thenReturn("Tenant 'HDFC_BANK' provisioned successfully.");

        mockMvc.perform(post("/api/v1/master/tenants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("POST /master/tenants: Non-SYSTEM_ADMIN is forbidden (403)")
    @WithMockUser(roles = "TENANT_ADMIN")
    void onboardBank_tenantAdmin_forbidden() throws Exception {
        TenantOnboardRequestDto dto = new TenantOnboardRequestDto();
        dto.setTenantCode("SBI_BANK");
        dto.setBankName("State Bank");
        dto.setAdminUsername("admin");
        dto.setAdminEmail("admin@sbi.com");

        mockMvc.perform(post("/api/v1/master/tenants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /master/tenants: Duplicate tenant returns 409 Conflict")
    @WithMockUser(roles = "SYSTEM_ADMIN")
    void onboardBank_duplicateTenant_returnsConflict() throws Exception {
        TenantOnboardRequestDto dto = new TenantOnboardRequestDto();
        dto.setTenantCode("EXISTING_BANK");
        dto.setBankName("Existing Bank");
        dto.setAdminUsername("admin");
        dto.setAdminEmail("admin@existing.com");

        when(tenantProvisioningService.onboardNewBank(any()))
                .thenThrow(new AmlBusinessException("Tenant 'EXISTING_BANK' already exists.", HttpStatus.CONFLICT));

        mockMvc.perform(post("/api/v1/master/tenants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Tenant 'EXISTING_BANK' already exists."));
    }

    @Test
    @DisplayName("GET /master/tenants: System Admin lists registered tenants")
    @WithMockUser(roles = "SYSTEM_ADMIN")
    void listTenants_systemAdmin_success() throws Exception {
        when(tenantProvisioningService.listTenants()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/master/tenants"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
