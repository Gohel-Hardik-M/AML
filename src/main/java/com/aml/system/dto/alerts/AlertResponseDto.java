package com.aml.system.dto.alerts;

import com.aml.system.model.enums.AlertSeverity;
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
public class AlertResponseDto {
    private UUID alertId;
    private String tenantId;
    private UUID transactionId;
    private String customerId;
    private String ruleCode;
    private String ruleName;
    private AlertSeverity severity;
    private BigDecimal triggeredAmount;
    private String narrative;
    private Boolean isReviewed;
    private UUID assignedCaseId;
    private LocalDateTime createdAt;
}
