package com.aml.system.service;

import com.aml.system.dto.admin.ActivityDto;
import com.aml.system.repository.SystemAuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ActivityService {
    private final SystemAuditLogRepository repository;

    public Page<ActivityDto> list(Pageable pageable) {
        return repository.findAllByOrderByCreatedAtDesc(pageable).map(log -> new ActivityDto(
                log.getLogId(), log.getUserId(), log.getActionType(), log.getAffectedRecordId(),
                log.getIpAddress(), log.getDetails(), log.getCreatedAt()));
    }
}