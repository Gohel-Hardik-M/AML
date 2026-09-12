package com.aml.system.service;

import com.aml.system.ExcelTransactionReader.ExcelTransactionReader;
import com.aml.system.model.Batch;
import com.aml.system.model.BatchStatus;
import com.aml.system.model.TenantRuleConfig;
import com.aml.system.model.Transaction;
import com.aml.system.model.TransactionType;
import com.aml.system.multitenancy.TenantContextHolder;
import com.aml.system.exception.DuplicateBatchException;
import com.aml.system.exception.DuplicateTransactionException;
import com.aml.system.exception.NoActiveRulesException;
import com.aml.system.repository.BatchRepository;
import com.aml.system.repository.TenantRuleConfigRepository;
import com.aml.system.repository.TransactionRepository;
import com.aml.system.rule.RuleEngineService;
import com.aml.system.rule.RuleEvaluationResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BatchServiceTest {
    private static final String TENANT = "BANK_A";
    private static final LocalDateTime DATE = LocalDateTime.of(2026, 9, 11, 12, 0);

    @Mock BatchRepository batchRepository;
    @Mock TransactionRepository transactionRepository;
    @Mock ExcelTransactionReader reader;
    @Mock RuleEngineService ruleEngine;
    @Mock AlertService alertService;
    @Mock TenantRuleConfigRepository ruleConfigRepository;

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    void rejectsEmptyReaderResultBeforeCreatingBatch() throws Exception {
        BatchService service = service();
        TenantContextHolder.setTenantId(TENANT);
        when(reader.read(any())).thenReturn(List.of());

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.processUpload(file()));

        assertTrue(error.getMessage().contains("no transaction rows"));
        verify(batchRepository, never()).save(any());
        verify(transactionRepository, never()).saveAll(any());
    }

    @Test
    void rejectsDuplicateTransactionIdsInOneUpload() throws Exception {
        BatchService service = service();
        TenantContextHolder.setTenantId(TENANT);
        UUID id = UUID.randomUUID();
        when(reader.read(any())).thenReturn(List.of(transaction(id), transaction(id)));

        DuplicateTransactionException error = assertThrows(DuplicateTransactionException.class,
                () -> service.processUpload(file()));

        assertTrue(error.getMessage().contains("Duplicate transaction ID"));
        verify(batchRepository, never()).save(any());
    }

    @Test
    void rejectsTransactionAlreadyStored() throws Exception {
        BatchService service = service();
        TenantContextHolder.setTenantId(TENANT);
        UUID id = UUID.randomUUID();
        when(reader.read(any())).thenReturn(List.of(transaction(id)));
        when(transactionRepository.existsById(id)).thenReturn(true);

            DuplicateTransactionException error = assertThrows(DuplicateTransactionException.class,
                () -> service.processUpload(file()));

        assertTrue(error.getMessage().contains("already exists"));
        verify(batchRepository, never()).save(any());
    }

    @Test
    void rejectsDuplicateBatchDateAndChecksumBeforePersistence() throws Exception {
        BatchService service = service();
        TenantContextHolder.setTenantId(TENANT);
        when(reader.read(any())).thenReturn(List.of(transaction(UUID.randomUUID())));
        when(batchRepository.existsByTenantIdAndBatchDate(eq(TENANT), eq(LocalDate.of(2026, 9, 11)))).thenReturn(true);

            DuplicateBatchException error = assertThrows(DuplicateBatchException.class,
                () -> service.processUpload(file()));

        assertTrue(error.getMessage().contains("already exists for 2026-09-11"));
        verify(batchRepository, never()).save(any());
    }

    @Test
    void rejectsExactFileDuplicateWhenDateIsNew() throws Exception {
        BatchService service = service();
        TenantContextHolder.setTenantId(TENANT);
        when(reader.read(any())).thenReturn(List.of(transaction(UUID.randomUUID())));
        when(batchRepository.existsByTenantIdAndFileChecksum(eq(TENANT), anyString())).thenReturn(true);

        DuplicateBatchException error = assertThrows(DuplicateBatchException.class,
                () -> service.processUpload(file()));

        assertEquals("This exact file has already been uploaded.", error.getMessage());
        verify(batchRepository, never()).save(any());
    }

    @Test
    void failsWithoutEnabledRulesAndDoesNotCreateAlerts() throws Exception {
        BatchService service = service();
        TenantContextHolder.setTenantId(TENANT);
        when(reader.read(any())).thenReturn(List.of(transaction(UUID.randomUUID())));
        when(batchRepository.save(any(Batch.class))).thenAnswer(invocation -> {
            Batch batch = invocation.getArgument(0);
            batch.setId(UUID.randomUUID());
            return batch;
        });
        when(ruleConfigRepository.findByTenantId(TENANT)).thenReturn(List.of());

            NoActiveRulesException error = assertThrows(NoActiveRulesException.class,
                () -> service.processUpload(file()));

        assertTrue(error.getMessage().contains("No enabled AML rules"));
        verify(alertService, never()).createAlerts(any(), any());
    }

    @Test
    void processesRowsAndMarksBatchProcessedWhenRuleTriggers() throws Exception {
        BatchService service = service();
        TenantContextHolder.setTenantId(TENANT);
        Transaction transaction = transaction(UUID.randomUUID());
        when(reader.read(any())).thenReturn(List.of(transaction));
        when(transactionRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionRepository.findTransactionsBetween(any(), any())).thenReturn(List.of(transaction));
        when(batchRepository.save(any(Batch.class))).thenAnswer(invocation -> {
            Batch batch = invocation.getArgument(0);
            if (batch.getId() == null) batch.setId(UUID.randomUUID());
            return batch;
        });
        when(ruleConfigRepository.findByTenantId(TENANT)).thenReturn(List.of(
                TenantRuleConfig.builder().ruleCode("STRUCTURING_001").isEnabled(true).windowMinutes(60).build()));
        when(ruleEngine.evaluate(any(), anyMap(), any())).thenReturn(List.of(
                RuleEvaluationResult.triggered("STRUCTURING_001", "Structuring", "HIGH", BigDecimal.TEN, "triggered")));

        Batch result = service.processUpload(file());

        assertEquals(BatchStatus.PROCESSED, result.getStatus());
        assertEquals(TENANT, result.getTenantId());
        assertEquals(LocalDate.of(2026, 9, 11), result.getBatchDate());
        verify(alertService).createAlerts(eq(transaction), anyList());
        verify(transactionRepository).saveAll(any());
    }

    @Test
    void marksBatchReviewedWhenNoRuleTriggers() throws Exception {
        BatchService service = service();
        TenantContextHolder.setTenantId(TENANT);
        Transaction transaction = transaction(UUID.randomUUID());
        when(reader.read(any())).thenReturn(List.of(transaction));
        when(transactionRepository.saveAll(any())).thenReturn(List.of(transaction));
        when(transactionRepository.findTransactionsBetween(any(), any())).thenReturn(List.of(transaction));
        when(batchRepository.save(any(Batch.class))).thenAnswer(invocation -> {
            Batch batch = invocation.getArgument(0);
            if (batch.getId() == null) batch.setId(UUID.randomUUID());
            return batch;
        });
        when(ruleConfigRepository.findByTenantId(TENANT)).thenReturn(List.of(
                TenantRuleConfig.builder().ruleCode("VELOCITY_001").isEnabled(true).windowMinutes(60).build()));
        when(ruleEngine.evaluate(any(), anyMap(), any())).thenReturn(List.of());

        Batch result = service.processUpload(file());

        assertEquals(BatchStatus.REVIEWED, result.getStatus());
        verify(alertService, never()).createAlerts(any(), any());
    }

    @Test
    void propagatesAlertFailureSoTransactionalCallerCanRollbackAllWrites() throws Exception {
        BatchService service = service();
        TenantContextHolder.setTenantId(TENANT);
        Transaction transaction = transaction(UUID.randomUUID());
        when(reader.read(any())).thenReturn(List.of(transaction));
        when(transactionRepository.saveAll(any())).thenReturn(List.of(transaction));
        when(transactionRepository.findTransactionsBetween(any(), any())).thenReturn(List.of(transaction));
        when(batchRepository.save(any(Batch.class))).thenAnswer(invocation -> {
            Batch batch = invocation.getArgument(0);
            batch.setId(UUID.randomUUID());
            return batch;
        });
        when(ruleConfigRepository.findByTenantId(TENANT)).thenReturn(List.of(
                TenantRuleConfig.builder().ruleCode("STRUCTURING_001").isEnabled(true).windowMinutes(60).build()));
        when(ruleEngine.evaluate(any(), anyMap(), any())).thenReturn(List.of(
                RuleEvaluationResult.triggered("STRUCTURING_001", "Structuring", "HIGH", BigDecimal.TEN, "triggered")));
        doThrow(new RuntimeException("alert insert failed")).when(alertService).createAlerts(any(), any());

        RuntimeException error = assertThrows(RuntimeException.class, () -> service.processUpload(file()));

        assertEquals("alert insert failed", error.getMessage());
        verify(batchRepository, never()).save(argThat(batch -> batch.getStatus() == BatchStatus.PROCESSED));
    }

    private BatchService service() {
        return new BatchService(batchRepository, transactionRepository, reader, ruleEngine, alertService, ruleConfigRepository);
    }

    private Transaction transaction(UUID id) {
        return Transaction.builder().transactionId(id).sourceAccountId("SRC").destinationAccountId("DST")
                .customerID("CUSTOMER").amount(BigDecimal.TEN).currency("INR")
                .transactionType(TransactionType.WIRE_TRANSFER).timestamp(DATE).build();
    }

    private MockMultipartFile file() {
        return new MockMultipartFile("file", "transactions.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[]{1, 2, 3});
    }
}
