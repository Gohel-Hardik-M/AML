package com.aml.system.service;

import com.aml.system.ExcelTransactionReader.ExcelTransactionReader;
import com.aml.system.model.TenantRuleConfig;
import com.aml.system.model.Transaction;
import com.aml.system.model.TransactionType;
import com.aml.system.multitenancy.TenantContextHolder;
import com.aml.system.exception.NoActiveRulesException;
import com.aml.system.repository.BatchRepository;
import com.aml.system.repository.TransactionRepository;
import com.aml.system.repository.TenantRuleConfigRepository;
import com.aml.system.rule.RuleEngineService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
class BatchServiceRollbackIntegrationTest {
    private static final String TENANT = "HDFC_BANK";

    @Autowired BatchService batchService;
    @Autowired BatchRepository batchRepository;
    @Autowired TransactionRepository transactionRepository;

    @MockBean ExcelTransactionReader reader;
    @MockBean TenantRuleConfigRepository ruleConfigRepository;
    @MockBean RuleEngineService ruleEngineService;
    @MockBean AlertService alertService;

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    void removesBatchAndTransactionsWhenProcessingFailsAfterPersistence() throws Exception {
        TenantContextHolder.setTenantId(TENANT);
        UUID transactionId = UUID.randomUUID();
        Transaction transaction = Transaction.builder()
                .transactionId(transactionId)
                .sourceAccountId("SRC")
                .destinationAccountId("DST")
                .customerID("ROLLBACK-TEST")
                .amount(BigDecimal.TEN)
                .currency("INR")
                .transactionType(TransactionType.WIRE_TRANSFER)
                .timestamp(LocalDateTime.now().minusMinutes(1))
                .build();
        when(reader.read(any())).thenReturn(List.of(transaction));
        when(ruleConfigRepository.findByTenantId(TENANT)).thenReturn(List.<TenantRuleConfig>of());

            assertThrows(NoActiveRulesException.class,
                () -> batchService.processUpload(new MockMultipartFile("file", "rollback.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[]{7, 8, 9})));

        assertFalse(transactionRepository.existsById(transactionId));
    }
}
