package com.aml.system.dto.batch;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchUploadRequestDto {
    private String tenantId;
    private String fileName;
    private String fileChecksum;
    private String channel;
    private Long recordCountEstimate;
}
