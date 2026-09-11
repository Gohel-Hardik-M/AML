package com.aml.system.service;

import com.aml.system.model.SystemAuditLog;
import com.aml.system.repository.SystemAuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final SystemAuditLogRepository auditLogRepository;

    public void logAction(String userId, String actionType, String affectedRecordId, String details, HttpServletRequest request) {
        String ipAddress = (request != null) ? request.getRemoteAddr() : "SYSTEM";

        SystemAuditLog audit = SystemAuditLog.builder()
                .userId(userId)
                .actionType(actionType)
                .affectedRecordId(affectedRecordId)
                .ipAddress(ipAddress)
                .details(details)
                .build();

        try {
            auditLogRepository.save(audit);
            log.info("AUDIT: User [{}] executed [{}] on Record [{}] from IP [{}]", userId, actionType, affectedRecordId, ipAddress);
        } catch (Exception e) {
            // Fallback: if DB save fails, ensure the audit event is at least captured in logs.
            // This prevents audit logging failures from killing parent operations (e.g., a successful login).
            log.error("AUDIT_DB_FAILURE: Failed to persist audit log. Falling back to file log. " +
                       "User=[{}], Action=[{}], Record=[{}], IP=[{}], Details=[{}], Error=[{}]",
                    userId, actionType, affectedRecordId, ipAddress, details, e.getMessage());
        }
    }
}