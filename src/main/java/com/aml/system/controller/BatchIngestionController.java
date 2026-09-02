package com.aml.system.controller;

import com.aml.system.dto.ApiResponse;
import com.aml.system.dto.batch.BatchJobResponseDto;
import com.aml.system.dto.batch.BatchUploadRequestDto;
import com.aml.system.service.AmlBatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
@Slf4j
@RestController
@RequestMapping("/api/v1/batch")
@RequiredArgsConstructor
public class BatchIngestionController {

    private final AmlBatchService amlBatchService;

    @PostMapping("/ingest")
    public ResponseEntity<ApiResponse<BatchJobResponseDto>> triggerBatchIngestion(
            @RequestParam("filePath") String filePath,
            @RequestBody BatchUploadRequestDto requestDto
    ) {
        log.info("Received request to ingest batch file: {} for tenant: {}", filePath, requestDto.getTenantId());
        BatchJobResponseDto response = amlBatchService.launchBatchJob(filePath, requestDto);
        return ResponseEntity.accepted().body(ApiResponse.success(response, "Batch ingestion job submitted successfully"));
    }

    @PostMapping("/elt/{batchId}")
    public ResponseEntity<ApiResponse<String>> triggerEltProcedure(@PathVariable UUID batchId) {
        log.info("Received request to trigger PostgreSQL ELT pipeline for batch ID: {}", batchId);
        amlBatchService.executeEltPipeline(batchId);
        return ResponseEntity.ok(ApiResponse.success("PostgreSQL ELT procedure CALL process_batch_transactions triggered in background", "ELT Pipeline Started"));
    }
}