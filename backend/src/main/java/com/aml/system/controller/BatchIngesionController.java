package com.aml.system.controller;

import com.aml.system.model.Batch;
import com.aml.system.dto.admin.BatchSummaryDto;
import com.aml.system.service.BatchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.access.prepost.PreAuthorize;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;


@RestController
@RequestMapping("/api/v1/transactions")
public class BatchIngesionController {

        private final BatchService batchService;

        public BatchIngesionController(BatchService batchService) {
            this.batchService = batchService;
        }

        @PostMapping("/upload")
        @PreAuthorize("hasRole('TENANT_ADMIN')")
        public ResponseEntity<String> uploadExcelBatch(
                @RequestParam("file") MultipartFile file) {

            if (file.isEmpty() || file.getOriginalFilename() == null || file.getOriginalFilename().isBlank()) {
                return ResponseEntity.badRequest()
                        .body("An XLS or XLSX file is required.");
            }
            String fileName = file.getOriginalFilename().toLowerCase(Locale.ROOT);
            if (!(fileName.endsWith(".xls") || fileName.endsWith(".xlsx"))) {
                return ResponseEntity.badRequest().body("Only XLS and XLSX files are supported.");
            }
            if (file.getSize() > 100L * 1024 * 1024) {
                return ResponseEntity.badRequest().body("File size must be 100 MB or less.");
            }

            try {

                Batch savedBatch = batchService.processUpload(file);

                return ResponseEntity.ok(
                        "Excel file received successfully. Batch ID: "
                                + savedBatch.getId()
                );

            } catch (Exception e) {

                return ResponseEntity.badRequest()
                        .body("Excel rejected: " + e.getMessage());
            }
        }

        @GetMapping("/batches")
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<Page<BatchSummaryDto>> listBatches(Pageable pageable) {
            return ResponseEntity.ok(batchService.listBatches(pageable));
        }

}
