package com.aml.system.controller;

import com.aml.system.model.Batch;
import com.aml.system.model.BatchStatus;
import com.aml.system.service.BatchService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BatchIngestionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BatchService batchService;

    @Test
    @DisplayName("POST /upload: Tenant Admin successfully uploads valid Excel file")
    @WithMockUser(roles = "TENANT_ADMIN")
    void upload_tenantAdmin_success() throws Exception {
        UUID batchId = UUID.randomUUID();
        Batch batch = Batch.builder()
                .id(batchId)
                .fileName("tx_2026.xlsx")
                .batchDate(LocalDate.now())
                .status(BatchStatus.PROCESSED)
                .uploadedAt(LocalDateTime.now())
                .build();

        when(batchService.processUpload(any())).thenReturn(batch);

        MockMultipartFile file = new MockMultipartFile(
                "file", "tx_2026.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                new byte[]{1, 2, 3, 4}
        );

        mockMvc.perform(multipart("/api/v1/transactions/upload").file(file))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(batchId.toString())));
    }

    @Test
    @DisplayName("POST /upload: Compliance Officer is forbidden (403) from uploading batches")
    @WithMockUser(roles = "COMPLIANCE_OFFICER")
    void upload_complianceOfficer_forbidden() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "tx.xlsx", "application/vnd.ms-excel", new byte[]{1, 2}
        );

        mockMvc.perform(multipart("/api/v1/transactions/upload").file(file))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /upload: Rejects non-Excel extensions (e.g. .csv, .pdf)")
    @WithMockUser(roles = "TENANT_ADMIN")
    void upload_invalidExtension_badRequest() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "transactions.csv", "text/csv", new byte[]{1, 2, 3}
        );

        mockMvc.perform(multipart("/api/v1/transactions/upload").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Only XLS and XLSX files are supported")));
    }

    @Test
    @DisplayName("POST /upload: Rejects empty file payload")
    @WithMockUser(roles = "TENANT_ADMIN")
    void upload_emptyFile_badRequest() throws Exception {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file", "empty.xlsx", "application/vnd.ms-excel", new byte[]{}
        );

        mockMvc.perform(multipart("/api/v1/transactions/upload").file(emptyFile))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("An XLS or XLSX file is required")));
    }

    @Test
    @DisplayName("GET /batches: Authenticated user can list batches")
    @WithMockUser(roles = "TENANT_ADMIN")
    void listBatches_authenticated_success() throws Exception {
        when(batchService.listBatches(any())).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/v1/transactions/batches"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /batches: Unauthenticated request is rejected (401)")
    void listBatches_unauthenticated_unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/transactions/batches"))
                .andExpect(status().isUnauthorized());
    }
}
