package com.aml.system.service;

import com.aml.system.ExcelTransactionReader.ExcelTransactionReader;
import com.aml.system.model.Batch;
import com.aml.system.model.BatchStatus;
import com.aml.system.exception.DuplicateBatchException;
import com.aml.system.exception.DuplicateTransactionException;
import com.aml.system.exception.NoActiveRulesException;
import com.aml.system.exception.TenantRoutingException;
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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

        @Transactional(rollbackOn = Exception.class)
        public Batch processUpload(MultipartFile file) throws IOException {

                        String tenantId = TenantContextHolder.getTenantId();
                        if (tenantId == null || tenantId.isBlank()) {
                                throw new TenantRoutingException("Tenant context is missing. Login as a bank administrator before uploading.");
                        }

            List<Transaction> transactions = excelTransactionReader.read(file);
                        if (transactions.isEmpty()) {
                                throw new IllegalArgumentException("The Excel file contains no transaction rows.");
                        }
                        Set<UUID> transactionIds = new HashSet<>();
                        for (Transaction transaction : transactions) {
                                if (!transactionIds.add(transaction.getTransactionId())) {
                                               throw new DuplicateTransactionException("Duplicate transaction ID '" + transaction.getTransactionId()
                                                        + "' found in the uploaded file.");
                                }
                                if (transactionRepository.existsById(transaction.getTransactionId())) {
                                               throw new DuplicateTransactionException("Transaction ID '" + transaction.getTransactionId()
                                                        + "' already exists. The transaction was not uploaded again.");
                                }
                        }

                        LocalDate batchDate = transactions.stream()
                                        .map(Transaction::getTimestamp)
                                        .map(LocalDateTime::toLocalDate)
                                        .min(LocalDate::compareTo)
                                        .orElseThrow(() -> new IllegalArgumentException("The upload has no valid transaction date."));
                        String fileChecksum = sha256(file.getBytes());
                        if (batchRepository.existsByTenantIdAndBatchDate(tenantId, batchDate)) {
                                throw new DuplicateBatchException("A batch for tenant '" + tenantId + "' already exists for " + batchDate + ".");
                        }
                        if (batchRepository.existsByTenantIdAndFileChecksum(tenantId, fileChecksum)) {
                                       throw new DuplicateBatchException("This exact file has already been uploaded.");
                        }

            Batch batch = Batch.builder()
                                        .tenantId(tenantId)
                                        .batchDate(batchDate)
                                        .fileChecksum(fileChecksum)
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
                        throw new NoActiveRulesException(
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

        private String sha256(byte[] content) {
                try {
                        byte[] digest = MessageDigest.getInstance("SHA-256").digest(content);
                        StringBuilder result = new StringBuilder(64);
                        for (byte value : digest) {
                                result.append(String.format("%02x", value));
                        }
                        return result.toString();
                } catch (NoSuchAlgorithmException exception) {
                        throw new IllegalStateException("SHA-256 is not available.", exception);
                }
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
