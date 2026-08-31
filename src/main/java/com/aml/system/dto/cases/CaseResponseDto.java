package com.aml.system.dto.cases;

import com.aml.system.model.enums.AlertSeverity;
import com.aml.system.model.enums.CaseStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaseResponseDto {
    private UUID caseId;
    private String tenantId;
    private String customerId;
    private String customerName;
    private CaseStatus status;
    private AlertSeverity riskLevel;
    private String assignedAnalystId;
    private String primaryRuleTriggered;
    private BigDecimal totalSuspiciousAmount;
    private String investigationNotes;
    private String sarFilingReference;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
