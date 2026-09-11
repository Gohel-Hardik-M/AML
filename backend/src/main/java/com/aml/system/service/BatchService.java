package com.aml.system.service;

import com.aml.system.ExcelTransactionReader.ExcelTransactionReader;
import com.aml.system.model.Batch;
import com.aml.system.model.BatchStatus;
import com.aml.system.dto.admin.BatchSummaryDto;
import com.aml.system.model.Transaction;
import com.aml.system.multitenancy.TenantContextHolder;
import com.aml.system.model.TenantRuleConfig;
import com.aml.system.repository.BatchRepository;
import com.aml.system.repository.TransactionRepository;
import com.aml.system.repository.TenantRuleConfigRepository;
import com.aml.system.rule.RuleEngineService;
import com.aml.system.rule.RuleEvaluationResult;
import com.aml.system.rule.RuleEvaluationContext;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
@Slf4j
@Service
public class BatchService {

        private final BatchRepository batchRepository;
        private final TransactionRepository transactionRepository;
        private final ExcelTransactionReader excelTransactionReader;
        private final RuleEngineService ruleEngineService;
        private final AlertService alertService;
        private final TenantRuleConfigRepository tenantRuleConfigRepository;

        public BatchService(
                BatchRepository batchRepository,
                TransactionRepository transactionRepository,
                ExcelTransactionReader excelTransactionReader,
                RuleEngineService ruleEngineService,
                AlertService alertService,
                TenantRuleConfigRepository tenantRuleConfigRepository) {

            this.batchRepository = batchRepository;
            this.transactionRepository = transactionRepository;
            this.excelTransactionReader = excelTransactionReader;
            this.ruleEngineService = ruleEngineService;
            this.alertService = alertService;
            this.tenantRuleConfigRepository = tenantRuleConfigRepository;
        }

        @Transactional
        public Batch processUpload(MultipartFile file) throws IOException {

                        String tenantId = TenantContextHolder.getTenantId();
                        if (tenantId == null || tenantId.isBlank()) {
                                throw new IllegalStateException("Tenant context is missing. Login as a bank administrator before uploading.");
                        }

            List<Transaction> transactions = excelTransactionReader.read(file);

            Batch batch = Batch.builder()
                    .fileName(file.getOriginalFilename())
                    .status(BatchStatus.PENDING)
                    .uploadedAt(LocalDateTime.now())
                    .build();

            Batch savedBatch = batchRepository.save(batch);

            transactions.forEach(transaction -> transaction.setBatchId(savedBatch.getId()));
            List<Transaction> savedTransactions = transactionRepository.saveAll(transactions);
            int maxWindowMinutes = activeWindowMinutes(tenantId);
            LocalDateTime historyFrom = savedTransactions.stream()
                    .map(Transaction::getTimestamp)
                    .min(LocalDateTime::compareTo)
                    .orElse(LocalDateTime.now())
                    .minusMinutes(maxWindowMinutes);
            LocalDateTime historyTo = savedTransactions.stream()
                    .map(Transaction::getTimestamp)
                    .max(LocalDateTime::compareTo)
                    .orElse(LocalDateTime.now());
            List<Transaction> contextTransactions = transactionRepository.findTransactionsBetween(historyFrom, historyTo);
            Map<String, List<Transaction>> transactionsByCustomer = contextTransactions.stream()
                    .collect(Collectors.groupingBy(Transaction::getCustomerID));
            RuleEvaluationContext evaluationContext = new RuleEvaluationContext(transactionsByCustomer);
            Map<String, TenantRuleConfig> activeConfigs = tenantRuleConfigRepository.findByTenantId(
                            tenantId).stream()
                    .filter(config -> Boolean.TRUE.equals(config.getIsEnabled()))
                    .collect(Collectors.toMap(TenantRuleConfig::getRuleCode, Function.identity(), (first, second) -> first));

            if (activeConfigs.isEmpty()) {
                throw new IllegalStateException(
                        "No enabled AML rules are configured for tenant '" + tenantId
                                + "'. Allocate and enable rules before uploading transactions.");
            }

            int alertCount = 0;
            for (Transaction savedTransaction : savedTransactions) {

                List<RuleEvaluationResult> results = ruleEngineService.evaluate(
                        savedTransaction, activeConfigs, evaluationContext);

                if (!results.isEmpty()) {
                    alertService.createAlerts(savedTransaction, results);
                    alertCount += results.size();
                }
            }

            savedBatch.setStatus(alertCount == 0 ? BatchStatus.REVIEWED : BatchStatus.PROCESSED);
            batchRepository.save(savedBatch);
            log.info("Processed batch '{}' with {} transactions and {} alerts using {} active rules.",
                    savedBatch.getId(), savedTransactions.size(), alertCount, activeConfigs.size());
            return savedBatch;
        }

        private int activeWindowMinutes(String tenantId) {
                return tenantRuleConfigRepository.findByTenantId(tenantId).stream()
                        .filter(config -> Boolean.TRUE.equals(config.getIsEnabled()))
                        .map(TenantRuleConfig::getWindowMinutes)
                        .filter(java.util.Objects::nonNull)
                        .filter(value -> value > 0)
                        .max(Integer::compareTo)
                        .orElse(1);
        }

        public List<BatchSummaryDto> listBatches() {
                return batchRepository.findAllByOrderByUploadedAtDesc().stream()
                        .map(batch -> new BatchSummaryDto(
                                batch.getId(),
                                batch.getFileName(),
                                batch.getStatus(),
                                batch.getUploadedAt()))
                        .toList();
        }

        public Page<BatchSummaryDto> listBatches(Pageable pageable) {
                return batchRepository.findAllByOrderByUploadedAtDesc(pageable)
                        .map(batch -> new BatchSummaryDto(batch.getId(), batch.getFileName(), batch.getStatus(), batch.getUploadedAt()));
        }

}
