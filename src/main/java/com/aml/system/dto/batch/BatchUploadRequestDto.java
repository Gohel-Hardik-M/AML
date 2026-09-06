package com.aml.system.dto.batch;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchUploadRequestDto {

    // tenantId REMOVED: Handled securely via JWT and TenantContextHolder
    // fileName & fileChecksum REMOVED: Handled backend-side via MultipartFile

    @NotNull(message = "Batch date is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) // Ensures the frontend sends format: YYYY-MM-DD
    private LocalDate batchDate;

    @NotBlank(message = "Channel is required (e.g., PORTAL, API, SFTP)")
    @Size(max = 32, message = "Channel must not exceed 32 characters")
    private String channel;
}