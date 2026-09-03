package com.aml.system.controller;

import com.aml.system.dto.ApiResponse;
import com.aml.system.dto.batch.BatchJobResponseDto;
import com.aml.system.dto.batch.BatchUploadRequestDto;
import com.aml.system.service.AmlBatchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/batch")
@RequiredArgsConstructor
public class BatchIngestionController {

    private final AmlBatchService amlBatchService;

    @PostMapping("/ingest")
    public ResponseEntity<ApiResponse<BatchJobResponseDto>> triggerBatchIngestion(
            @RequestParam("filePath") @NotBlank(message = "File path is required") @Size(max = 1000, message = "File path must not exceed 1000 characters") String filePath,
            @Valid @RequestBody BatchUploadRequestDto requestDto
    ) {
        log.info("Received request to ingest batch file: {} for tenant: {}", filePath, requestDto.getTenantId());
        BatchJobResponseDto response = amlBatchService.launchBatchJob(filePath, requestDto);
        return ResponseEntity.accepted().body(ApiResponse.success(response, "Batch ingestion job submitted successfully"));
    }

    @PostMapping("/elt/{batchId}")
    public ResponseEntity<ApiResponse<String>> triggerEltProcedure(@PathVariable UUID batchId) {
        log.info("Received request to trigger PostgreSQL ELT pipeline for batch ID: {}", batchId);
        String tenantId = com.aml.system.multitenancy.TenantContextHolder.getTenantId();
        amlBatchService.executeEltPipeline(batchId, tenantId);
        return ResponseEntity.ok(ApiResponse.success("PostgreSQL ELT procedure CALL process_batch_transactions triggered in background", "ELT Pipeline Started"));
    }
}
