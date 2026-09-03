package com.aml.system.dto.batch;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchUploadRequestDto {
    @NotBlank(message = "Tenant ID is required")
    @Size(max = 64, message = "Tenant ID must not exceed 64 characters")
    @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "Tenant ID contains invalid characters")
    private String tenantId;

    @NotBlank(message = "File name is required")
    @Size(max = 255, message = "File name must not exceed 255 characters")
    private String fileName;

    @NotBlank(message = "File checksum is required")
    @Pattern(regexp = "^[A-Fa-f0-9]{64}$", message = "File checksum must be a SHA-256 hexadecimal value")
    private String fileChecksum;

    @NotBlank(message = "Channel is required")
    @Size(max = 32, message = "Channel must not exceed 32 characters")
    private String channel;

    @PositiveOrZero(message = "Record count estimate cannot be negative")
    private Long recordCountEstimate;
}
