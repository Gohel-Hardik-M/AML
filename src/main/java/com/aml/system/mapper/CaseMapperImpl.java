package com.aml.system.mapper;

import com.aml.system.dto.cases.CaseResponseDto;
import com.aml.system.model.CaseEntity;
import org.springframework.stereotype.Component;

@Component
public class CaseMapperImpl implements CaseMapper {

    @Override
    public CaseResponseDto toDto(CaseEntity entity) {
        if (entity == null) return null;
        return CaseResponseDto.builder()
                .caseId(entity.getCaseId())
                .tenantId(entity.getTenantId())
                .customerId(entity.getCustomerId())
                .customerName(entity.getCustomerName())
                .status(entity.getStatus())
                .riskLevel(entity.getRiskLevel())
                .assignedAnalystId(entity.getAssignedAnalystId())
                .primaryRuleTriggered(entity.getPrimaryRuleTriggered())
                .totalSuspiciousAmount(entity.getTotalSuspiciousAmount())
                .investigationNotes(entity.getInvestigationNotes())
                .sarFilingReference(entity.getSarFilingReference())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    @Override
    public CaseEntity toEntity(CaseResponseDto dto) {
        if (dto == null) return null;
        return CaseEntity.builder()
                .caseId(dto.getCaseId())
                .tenantId(dto.getTenantId())
                .customerId(dto.getCustomerId())
                .customerName(dto.getCustomerName())
                .status(dto.getStatus())
                .riskLevel(dto.getRiskLevel())
                .assignedAnalystId(dto.getAssignedAnalystId())
                .primaryRuleTriggered(dto.getPrimaryRuleTriggered())
                .totalSuspiciousAmount(dto.getTotalSuspiciousAmount())
                .investigationNotes(dto.getInvestigationNotes())
                .sarFilingReference(dto.getSarFilingReference())
                .build();
    }
}
