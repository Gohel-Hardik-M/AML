package com.aml.system.service;

import com.aml.system.dto.alerts.AlertResponseDto;
import com.aml.system.dto.cases.CaseResponseDto;
import com.aml.system.exception.AmlBusinessException;
import com.aml.system.exception.ErrorCodeEnum;
import com.aml.system.mapper.AlertMapper;
import com.aml.system.mapper.CaseMapper;
import com.aml.system.model.Alert;
import com.aml.system.model.CaseEntity;
import com.aml.system.model.enums.AlertSeverity;
import com.aml.system.model.enums.CaseStatus;
import com.aml.system.multitenancy.TenantContext;
import com.aml.system.repository.AlertRepository;
import com.aml.system.repository.CaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertService {

    private final AlertRepository alertRepository;
    private final CaseRepository caseRepository;
    private final AlertMapper alertMapper;
    private final CaseMapper caseMapper;

    @Transactional(readOnly = true)
    public Page<AlertResponseDto> getAlerts(AlertSeverity severity, Boolean isReviewed, Pageable pageable) {
        String tenantId = TenantContext.getTenantId();
        Page<Alert> page;
        if (severity != null) {
            page = alertRepository.findByTenantIdAndSeverity(tenantId, severity, pageable);
        } else if (isReviewed != null) {
            page = alertRepository.findByTenantIdAndIsReviewed(tenantId, isReviewed, pageable);
        } else {
            page = alertRepository.findAll(pageable);
        }
        return page.map(alertMapper::toDto);
    }

    @Transactional
    public CaseResponseDto escalateAlertToCase(UUID alertId, String analystId) {
        String tenantId = TenantContext.getTenantId();
        Alert alert = alertRepository.findByAlertIdAndTenantId(alertId, tenantId)
                .orElseThrow(() -> new AmlBusinessException(ErrorCodeEnum.ALERT_NOT_FOUND, "Alert not found for ID: " + alertId));

        CaseEntity newCase = CaseEntity.builder()
                .tenantId(tenantId)
                .customerId(alert.getCustomerId())
                .customerName("Customer-" + alert.getCustomerId())
                .status(CaseStatus.OPEN)
                .riskLevel(alert.getSeverity())
                .assignedAnalystId(analystId)
                .primaryRuleTriggered(alert.getRuleName())
                .totalSuspiciousAmount(alert.getTriggeredAmount())
                .investigationNotes("Escalated from Alert ID: " + alert.getAlertId() + ". Narrative: " + alert.getNarrative())
                .build();

        CaseEntity savedCase = caseRepository.save(newCase);

        alert.setIsReviewed(true);
        alert.setAssignedCaseId(savedCase.getCaseId());
        alertRepository.save(alert);

        log.info("Escalated Alert [{}] to Case [{}] by Analyst [{}]", alertId, savedCase.getCaseId(), analystId);
        return caseMapper.toDto(savedCase);
    }
}
