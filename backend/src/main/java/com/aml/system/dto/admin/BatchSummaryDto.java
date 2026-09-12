package com.aml.system.dto.admin;

import com.aml.system.model.BatchStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BatchSummaryDto {
    private UUID batchId;
    private String fileName;
    private BatchStatus status;
    private LocalDateTime uploadedAt;
    private UUID uploadedById;

    public BatchSummaryDto(UUID batchId, String fileName, BatchStatus status, LocalDateTime uploadedAt) {
        this(batchId, fileName, status, uploadedAt, null);
    }
}

