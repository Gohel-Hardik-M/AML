package com.aml.system.dto.batch;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchJobResponseDto {
    private UUID batchId;
    private Long springBatchJobExecutionId;
    private String tenantId;
    private String status;
    private long totalRecords;
    private long processedRecords;
    private long skippedRecords;
    private long alertsGenerated;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String executionDuration;
}
