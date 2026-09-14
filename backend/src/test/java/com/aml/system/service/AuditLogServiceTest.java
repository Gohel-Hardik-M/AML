package com.aml.system.service;

import com.aml.system.model.SystemAuditLog;
import com.aml.system.repository.SystemAuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock
    private SystemAuditLogRepository auditLogRepository;

    private PlatformTransactionManager testTransactionManager;
    private AuditLogService auditLogService;

    @BeforeEach
    void setUp() {
        testTransactionManager = new PlatformTransactionManager() {
            @Override
            public TransactionStatus getTransaction(TransactionDefinition definition) throws TransactionException {
                return new SimpleTransactionStatus();
            }

            @Override
            public void commit(TransactionStatus status) throws TransactionException {}

            @Override
            public void rollback(TransactionStatus status) throws TransactionException {}
        };
        auditLogService = new AuditLogService(auditLogRepository, testTransactionManager);
    }

    @Test
    @DisplayName("logAction successfully persists audit log")
    void logAction_persistsSuccessfully() {
        when(auditLogRepository.saveAndFlush(any(SystemAuditLog.class))).thenAnswer(i -> i.getArgument(0));

        assertDoesNotThrow(() ->
                auditLogService.logAction("user1", "LOGIN_SUCCESS", "rec123", "Details", null)
        );

        verify(auditLogRepository, times(1)).saveAndFlush(any(SystemAuditLog.class));
    }

    @Test
    @DisplayName("logAction suppresses database errors and falls back cleanly without throwing")
    void logAction_suppressesDatabaseErrors() {
        when(auditLogRepository.saveAndFlush(any(SystemAuditLog.class)))
                .thenThrow(new RuntimeException("Database relation aml_system_audit_logs does not exist"));

        assertDoesNotThrow(() ->
                auditLogService.logAction("superadmin", "MASTER_LOGIN_SUCCESS", "rec123", "Details", null)
        );

        verify(auditLogRepository, times(1)).saveAndFlush(any(SystemAuditLog.class));
    }
}
