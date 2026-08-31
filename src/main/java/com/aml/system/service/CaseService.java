package com.aml.system.service;

import com.aml.system.dto.cases.CaseResponseDto;
import com.aml.system.dto.cases.CaseUpdateRequestDto;
import com.aml.system.exception.AmlBusinessException;
import com.aml.system.exception.ErrorCodeEnum;
import com.aml.system.mapper.CaseMapper;
import com.aml.system.model.CaseEntity;
import com.aml.system.model.enums.CaseStatus;
import com.aml.system.multitenancy.TenantContext;
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
public class CaseService {

    private final CaseRepository caseRepository;
    private final CaseMapper caseMapper;

    @Transactional(readOnly = true)
    public Page<CaseResponseDto> getCases(CaseStatus status, Pageable pageable) {
        String tenantId = TenantContext.getTenantId();
        Page<CaseEntity> page = (status != null) ?
                caseRepository.findByTenantIdAndStatus(tenantId, status, pageable) :
                caseRepository.findAll(pageable);
        return page.map(caseMapper::toDto);
    }

    @Transactional(readOnly = true)
    public CaseResponseDto getCaseById(UUID caseId) {
        String tenantId = TenantContext.getTenantId();
        CaseEntity caseEntity = caseRepository.findByCaseIdAndTenantId(caseId, tenantId)
                .orElseThrow(() -> new AmlBusinessException(ErrorCodeEnum.CASE_NOT_FOUND, "Case not found for ID: " + caseId));
        return caseMapper.toDto(caseEntity);
    }

    @Transactional
    public CaseResponseDto updateCase(UUID caseId, CaseUpdateRequestDto updateRequest) {
        String tenantId = TenantContext.getTenantId();
        CaseEntity caseEntity = caseRepository.findByCaseIdAndTenantId(caseId, tenantId)
                .orElseThrow(() -> new AmlBusinessException(ErrorCodeEnum.CASE_NOT_FOUND, "Case not found for ID: " + caseId));

        if (updateRequest.getStatus() != null) {
            caseEntity.setStatus(updateRequest.getStatus());
        }
        if (updateRequest.getAssignedAnalystId() != null) {
            caseEntity.setAssignedAnalystId(updateRequest.getAssignedAnalystId());
        }
        if (updateRequest.getInvestigationNotes() != null) {
            caseEntity.setInvestigationNotes(updateRequest.getInvestigationNotes());
        }
        if (updateRequest.getSarFilingReference() != null) {
            caseEntity.setSarFilingReference(updateRequest.getSarFilingReference());
        }

        CaseEntity saved = caseRepository.save(caseEntity);
        log.info("Updated compliance case ID [{}] status to [{}] for tenant [{}]", caseId, saved.getStatus(), tenantId);
        return caseMapper.toDto(saved);
    }
}
