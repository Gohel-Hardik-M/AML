package com.aml.system.controller;

import com.aml.system.dto.admin.RuleConfigUpdateDto;
import com.aml.system.model.TenantRuleConfig;
import com.aml.system.service.RuleConfigService;
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

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RuleConfigControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RuleConfigService ruleConfigService;

    @Test
    @DisplayName("GET /bank-admin/rules: Tenant admin retrieves rule configurations")
    @WithMockUser(roles = "TENANT_ADMIN")
    void getTenantRules_success() throws Exception {
        TenantRuleConfig config = TenantRuleConfig.builder()
                .ruleCode("VELOCITY_001")
                .thresholdAmount(new BigDecimal("50000.00"))
                .isEnabled(true)
                .build();

        when(ruleConfigService.getTenantRules()).thenReturn(List.of(config));

        mockMvc.perform(get("/api/v1/bank-admin/rules"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].ruleCode").value("VELOCITY_001"));
    }

    @Test
    @DisplayName("GET /bank-admin/rules/{ruleCode}: Fetches single rule by code")
    @WithMockUser(roles = "TENANT_ADMIN")
    void getRuleByCode_success() throws Exception {
        TenantRuleConfig config = TenantRuleConfig.builder()
                .ruleCode("STRUCTURING_001")
                .thresholdAmount(new BigDecimal("100000.00"))
                .isEnabled(true)
                .build();

        when(ruleConfigService.getRuleByCode("STRUCTURING_001")).thenReturn(config);

        mockMvc.perform(get("/api/v1/bank-admin/rules/STRUCTURING_001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ruleCode").value("STRUCTURING_001"));
    }

    @Test
    @DisplayName("PUT /bank-admin/rules/{ruleCode}: Updates configuration with valid values")
    @WithMockUser(roles = "TENANT_ADMIN")
    void updateRuleConfig_validValues_success() throws Exception {
        RuleConfigUpdateDto dto = new RuleConfigUpdateDto();
        dto.setThresholdAmount(new BigDecimal("75000.00"));
        dto.setWindowMinutes(45);
        dto.setMaxCount(5);
        dto.setPercentageDeviation(new BigDecimal("25.50"));
        dto.setIsEnabled(true);

        TenantRuleConfig updated = TenantRuleConfig.builder()
                .ruleCode("VELOCITY_001")
                .thresholdAmount(dto.getThresholdAmount())
                .windowMinutes(dto.getWindowMinutes())
                .maxCount(dto.getMaxCount())
                .percentageDeviation(dto.getPercentageDeviation())
                .isEnabled(true)
                .build();

        when(ruleConfigService.updateRuleConfig(eq("VELOCITY_001"), any())).thenReturn(updated);

        mockMvc.perform(put("/api/v1/bank-admin/rules/VELOCITY_001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.windowMinutes").value(45));
    }

    @Test
    @DisplayName("PUT /bank-admin/rules/{ruleCode}: Rejects negative threshold amount (400)")
    @WithMockUser(roles = "TENANT_ADMIN")
    void updateRuleConfig_negativeThreshold_badRequest() throws Exception {
        RuleConfigUpdateDto dto = new RuleConfigUpdateDto();
        dto.setThresholdAmount(new BigDecimal("-10.00"));

        mockMvc.perform(put("/api/v1/bank-admin/rules/VELOCITY_001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /bank-admin/rules/{ruleCode}: Rejects deviation greater than 100 (400)")
    @WithMockUser(roles = "TENANT_ADMIN")
    void updateRuleConfig_excessiveDeviation_badRequest() throws Exception {
        RuleConfigUpdateDto dto = new RuleConfigUpdateDto();
        dto.setPercentageDeviation(new BigDecimal("150.00"));

        mockMvc.perform(put("/api/v1/bank-admin/rules/VELOCITY_001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }
}
