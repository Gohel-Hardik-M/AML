package com.aml.system.dto.batch;

import lombok.Builder;
import lombok.Data;
import java.util.UUID;

@Data
@Builder
public class BatchUploadResponse {
    private boolean success;
    private String message;
    private UUID batchId;
    private String filePath;
    private String status;
}