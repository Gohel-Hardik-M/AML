package com.aml.system.mapper;

import com.aml.system.dto.alerts.AlertResponseDto;
import com.aml.system.model.Alert;
import org.springframework.stereotype.Component;

@Component
public class AlertMapperImpl implements AlertMapper {

    @Override
    public AlertResponseDto toDto(Alert entity) {
        if (entity == null) return null;
        return AlertResponseDto.builder()
                .alertId(entity.getAlertId())
                .tenantId(entity.getTenantId())
                .transactionId(entity.getTransactionId())
                .customerId(entity.getCustomerId())
                .ruleCode(entity.getRuleCode())
                .ruleName(entity.getRuleName())
                .severity(entity.getSeverity())
                .triggeredAmount(entity.getTriggeredAmount())
                .narrative(entity.getNarrative())
                .isReviewed(entity.getIsReviewed())
                .assignedCaseId(entity.getAssignedCaseId())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    @Override
    public Alert toEntity(AlertResponseDto dto) {
        if (dto == null) return null;
        return Alert.builder()
                .alertId(dto.getAlertId())
                .tenantId(dto.getTenantId())
                .transactionId(dto.getTransactionId())
                .customerId(dto.getCustomerId())
                .ruleCode(dto.getRuleCode())
                .ruleName(dto.getRuleName())
                .severity(dto.getSeverity())
                .triggeredAmount(dto.getTriggeredAmount())
                .narrative(dto.getNarrative())
                .isReviewed(dto.getIsReviewed())
                .assignedCaseId(dto.getAssignedCaseId())
                .createdAt(dto.getCreatedAt())
                .build();
    }
}
