package com.aml.system.controller;

import com.aml.system.dto.admin.AlertAssignmentDto;
import com.aml.system.dto.compliance.AlertReviewRequestDto;
import com.aml.system.model.Alert;
import com.aml.system.model.AlertSeverity;
import com.aml.system.service.AlertAssignmentService;
import com.aml.system.service.AuditLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AlertAssignmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AlertAssignmentService alertAssignmentService;

    @MockBean
    private AuditLogService auditLogService;

    @Test
    @DisplayName("POST /bank-admin/alerts/assign: Tenant Admin successfully assigns alerts")
    @WithMockUser(username = "admin_user", roles = "TENANT_ADMIN")
    void assignAlerts_tenantAdmin_success() throws Exception {
        UUID officerId = UUID.randomUUID();
        UUID alertId = UUID.randomUUID();
        AlertAssignmentDto dto = new AlertAssignmentDto();
        dto.setOfficerId(officerId);
        dto.setAlertIds(List.of(alertId));

        when(alertAssignmentService.assignAlerts(any())).thenReturn("Assigned 1 alert(s)");

        mockMvc.perform(post("/api/v1/bank-admin/alerts/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("POST /bank-admin/alerts/assign: Compliance officer is forbidden (403)")
    @WithMockUser(roles = "COMPLIANCE_OFFICER")
    void assignAlerts_complianceOfficer_forbidden() throws Exception {
        AlertAssignmentDto dto = new AlertAssignmentDto();
        dto.setOfficerId(UUID.randomUUID());
        dto.setAlertIds(List.of(UUID.randomUUID()));

        mockMvc.perform(post("/api/v1/bank-admin/alerts/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /bank-admin/alerts/unassigned: Tenant Admin retrieves unassigned alerts")
    @WithMockUser(roles = "TENANT_ADMIN")
    void getUnassignedAlerts_tenantAdmin_success() throws Exception {
        when(alertAssignmentService.getUnassignedAlerts()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/bank-admin/alerts/unassigned"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("GET /compliance/alerts/my-alerts: Compliance Officer retrieves their alerts")
    @WithMockUser(username = "officer_jane", roles = "COMPLIANCE_OFFICER")
    void getMyAlerts_complianceOfficer_success() throws Exception {
        when(alertAssignmentService.getMyAlerts(eq("officer_jane"), any()))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/v1/compliance/alerts/my-alerts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("PUT /compliance/alerts/{alertId}/close: Compliance officer closes alert with notes")
    @WithMockUser(username = "officer_jane", roles = "COMPLIANCE_OFFICER")
    void closeMyAlert_withNotes_success() throws Exception {
        UUID alertId = UUID.randomUUID();
        AlertReviewRequestDto reviewRequest = new AlertReviewRequestDto();
        reviewRequest.setReviewNotes("Reviewed and confirmed false positive after checking KYC.");

        Alert closedAlert = Alert.builder()
                .alertId(alertId)
                .reviewed(true)
                .reviewDecision("CLOSED")
                .reviewNotes("Reviewed and confirmed false positive after checking KYC.")
                .build();

        when(alertAssignmentService.closeMyAlert(eq(alertId), eq("officer_jane"), any()))
                .thenReturn(closedAlert);

        mockMvc.perform(put("/api/v1/compliance/alerts/" + alertId + "/close")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reviewRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reviewed").value(true));
    }

    @Test
    @DisplayName("PUT /compliance/alerts/{alertId}/close: Fails with 400 when review notes are blank")
    @WithMockUser(username = "officer_jane", roles = "COMPLIANCE_OFFICER")
    void closeMyAlert_blankNotes_badRequest() throws Exception {
        UUID alertId = UUID.randomUUID();
        AlertReviewRequestDto reviewRequest = new AlertReviewRequestDto();
        reviewRequest.setReviewNotes("");

        mockMvc.perform(put("/api/v1/compliance/alerts/" + alertId + "/close")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reviewRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /compliance/alerts/{alertId}/pdf: Generates PDF document bytes")
    @WithMockUser(username = "officer_jane", roles = "COMPLIANCE_OFFICER")
    void generateAlertPdf_success() throws Exception {
        UUID alertId = UUID.randomUUID();
        AlertReviewRequestDto reviewRequest = new AlertReviewRequestDto();
        reviewRequest.setReviewNotes("Closing case report.");

        byte[] fakePdf = "%PDF-1.4 mock pdf content".getBytes();
        Alert alert = Alert.builder().alertId(alertId).build();
        AlertAssignmentService.PdfResult pdfResult = new AlertAssignmentService.PdfResult(alert, fakePdf);

        when(alertAssignmentService.closeMyAlertAndGeneratePdf(eq(alertId), eq("officer_jane"), any()))
                .thenReturn(pdfResult);

        mockMvc.perform(post("/api/v1/compliance/alerts/" + alertId + "/pdf")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reviewRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("aml-alert-" + alertId)));
    }
}
