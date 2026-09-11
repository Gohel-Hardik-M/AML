package com.aml.system.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

@Data
@AllArgsConstructor
public class TenantSummaryDto {
    private String tenantId;
    private String bankName;
    private Boolean isActive;
    private Instant createdAt;
}
