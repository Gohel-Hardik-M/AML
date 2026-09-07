package com.aml.system.controller;

import com.aml.system.model.Batch;
import com.aml.system.service.BatchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;


@RestController
@RequestMapping("/api/v1/transactions")
public class BatchIngesionController {
    
        private final BatchService batchService;

        public BatchIngesionController(BatchService batchService) {
            this.batchService = batchService;
        }

        @PostMapping("/upload")
        public ResponseEntity<String> uploadExcelBatch(
                @RequestParam("file") MultipartFile file) {

            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body("Excel is Empty");
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

}
