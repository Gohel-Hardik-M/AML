package com.aml.system.dto.admin;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO returned when listing Compliance Officers.
 * Does NOT include password or other sensitive fields.
 */
@Data
@Builder
public class ComplianceOfficerResponseDto {

    private UUID userId;
    private String tenantId;
    private String username;
    private String email;
    private String fullName;
    private String role;
    private Boolean isActive;
    private Boolean isLocked;
    private Instant createdAt;
}
