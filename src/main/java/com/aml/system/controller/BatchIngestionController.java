package com.aml.system.controller;

import com.aml.system.dto.batch.BatchUploadRequestDto;
import com.aml.system.dto.batch.BatchUploadResponse;
import com.aml.system.model.BatchEntity;
import com.aml.system.service.AmlBatchService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/batches")
public class BatchIngestionController {

    private final AmlBatchService amlBatchService;

    public BatchIngestionController(AmlBatchService amlBatchService) {
        this.amlBatchService = amlBatchService;
    }

    @PostMapping(value = "/upload", consumes = {"multipart/form-data"})
    public ResponseEntity<BatchUploadResponse> uploadBatchFile(
            @RequestParam("file") MultipartFile file,
            @Valid @ModelAttribute BatchUploadRequestDto requestDto) throws Exception {

        BatchEntity batch = amlBatchService.processAndUploadBatch(
                file,
                requestDto.getBatchDate(),
                requestDto.getChannel()
        );

        BatchUploadResponse response = BatchUploadResponse.builder()
                .success(true)
                .message("File uploaded securely to Cloudinary.")
                .batchId(batch.getBatchId())
                .filePath(batch.getFileName())
                .status(batch.getStatus())
                .build();

        return ResponseEntity.ok(response);
    }

    // THIS IS THE UPDATED METHOD
    @PostMapping("/{batchDate}/process")
    public ResponseEntity<?> processBatch(@PathVariable LocalDate batchDate) {
        try {
            // This now actively triggers the download and Spring Batch job!
            amlBatchService.triggerBatchProcessing(batchDate);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Processing triggered successfully for batch: " + batchDate
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "Failed to start batch process: " + e.getMessage()
            ));
        }
    }

    @GetMapping
    public ResponseEntity<?> listBatches() {
        return ResponseEntity.ok(Map.of("message", "List of all batches"));
    }

    @GetMapping("/{batchDate}")
    public ResponseEntity<?> getBatchDetails(@PathVariable LocalDate batchDate) {
        return ResponseEntity.ok(Map.of("message", "Details for batch: " + batchDate));
    }

    @GetMapping("/{batchDate}/errors")
    public ResponseEntity<?> getBatchErrors(@PathVariable LocalDate batchDate) {
        return ResponseEntity.ok(Map.of("message", "Errors for batch: " + batchDate));
    }

    @PostMapping("/{batchDate}/elt")
    public ResponseEntity<?> runEltOperation(@PathVariable LocalDate batchDate) {
        return ResponseEntity.ok(Map.of("message", "ELT operation started for batch: " + batchDate));
    }
}