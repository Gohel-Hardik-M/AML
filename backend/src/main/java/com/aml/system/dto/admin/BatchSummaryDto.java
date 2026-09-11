package com.aml.system.dto.admin;

import com.aml.system.model.BatchStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
public class BatchSummaryDto {
    private UUID batchId;
    private String fileName;
    private BatchStatus status;
    private LocalDateTime uploadedAt;
}
