package com.aml.system.controller;

import com.aml.system.dto.admin.TenantRuleAllocationDto;
import com.aml.system.service.TenantRuleAllocationService;
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
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TenantRuleAllocationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TenantRuleAllocationService allocationService;

    @Test
    @DisplayName("GET /master/rules/catalog: System Admin retrieves global catalog")
    @WithMockUser(roles = "SYSTEM_ADMIN")
    void getGlobalCatalog_success() throws Exception {
        when(allocationService.getGlobalCatalog()).thenReturn(List.of(
                Map.of("rule_code", "VELOCITY_001", "description", "Velocity check")
        ));

        mockMvc.perform(get("/api/v1/master/rules/catalog"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].rule_code").value("VELOCITY_001"));
    }

    @Test
    @DisplayName("GET /master/rules/tenant/{tenantId}: Fetches allocated rules for tenant")
    @WithMockUser(roles = "SYSTEM_ADMIN")
    void getAllocatedRules_success() throws Exception {
        when(allocationService.getAllocatedRules("BANK_A")).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/master/rules/tenant/BANK_A"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("POST /master/rules/allocate: Allocates rules to tenant")
    @WithMockUser(roles = "SYSTEM_ADMIN")
    void allocateRules_success() throws Exception {
        TenantRuleAllocationDto dto = new TenantRuleAllocationDto();
        dto.setTenantId("BANK_A");
        dto.setRuleCodes(List.of("VELOCITY_001", "STRUCTURING_001"));

        when(allocationService.allocateRulesToTenant(any())).thenReturn("Rules allocated successfully");

        mockMvc.perform(post("/api/v1/master/rules/allocate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Rules allocated successfully"));
    }

    @Test
    @DisplayName("DELETE /master/rules/tenant/{tenantId}/{ruleCode}: Deallocates rule")
    @WithMockUser(roles = "SYSTEM_ADMIN")
    void deallocateRule_success() throws Exception {
        when(allocationService.deallocateRule("BANK_A", "VELOCITY_001")).thenReturn("Rule deallocated successfully");

        mockMvc.perform(delete("/api/v1/master/rules/tenant/BANK_A/VELOCITY_001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Rule deallocated successfully"));
    }

    @Test
    @DisplayName("GET /master/rules/catalog: Forbidden for Tenant Admin (403)")
    @WithMockUser(roles = "TENANT_ADMIN")
    void getGlobalCatalog_forbiddenForTenantAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/master/rules/catalog"))
                .andExpect(status().isForbidden());
    }
}
