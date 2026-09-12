package com.aml.system.controller;

import com.aml.system.dto.admin.ActivityDto;
import com.aml.system.service.ActivityService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ActivityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ActivityService activityService;

    @Test
    @DisplayName("GET /bank-admin/activity: Tenant Admin retrieves paginated activity log")
    @WithMockUser(roles = "TENANT_ADMIN")
    void listActivity_tenantAdmin_success() throws Exception {
        ActivityDto log = new ActivityDto(
                UUID.randomUUID(),
                "admin_icci",
                "OFFICER_CREATED",
                "record-1",
                "127.0.0.1",
                "Created compliance officer jane",
                java.time.Instant.now()
        );

        when(activityService.list(any())).thenReturn(new PageImpl<>(List.of(log)));

        mockMvc.perform(get("/api/v1/bank-admin/activity?page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].actionType").value("OFFICER_CREATED"));
    }

    @Test
    @DisplayName("GET /bank-admin/activity: Compliance Officer is forbidden (403)")
    @WithMockUser(roles = "COMPLIANCE_OFFICER")
    void listActivity_complianceOfficer_forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/bank-admin/activity"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /bank-admin/activity: Unauthenticated request is rejected (401)")
    void listActivity_unauthenticated_unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/bank-admin/activity"))
                .andExpect(status().isUnauthorized());
    }
}
