package com.aml.system.service;

import com.aml.system.ExcelTransactionReader.ExcelTransactionReader;
import com.aml.system.model.Batch;
import com.aml.system.model.BatchStatus;
import com.aml.system.model.Transaction;
import com.aml.system.multitenancy.TenantContextHolder;
import com.aml.system.repository.BatchRepository;
import com.aml.system.repository.TransactionRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.aml.system.multitenancy.TenantContextHolder.getTenantId;


@Service
public class BatchService {

        private final BatchRepository batchRepository;
        private final TransactionRepository transactionRepository;
        private final ExcelTransactionReader excelTransactionReader;

        public BatchService(
                BatchRepository batchRepository,
                TransactionRepository transactionRepository,
                ExcelTransactionReader excelTransactionReader) {

            this.batchRepository = batchRepository;
            this.transactionRepository = transactionRepository;
            this.excelTransactionReader = excelTransactionReader;
        }

        @Transactional
        public Batch processUpload(MultipartFile file) throws IOException {
            String tenantIdString = TenantContextHolder.getTenantId();
            UUID tenantId = UUID.fromString(tenantIdString);


            // 1. Read + validate entire Excel
            List<Transaction> transactions =
                    excelTransactionReader.read(file);

            // 2. Create Batch
            Batch batch = Batch.builder()
                    .fileName(file.getOriginalFilename())
                    .status(BatchStatus.PENDING)
                    .uploadedAt(LocalDateTime.now())
                    .uploadedById(tenantId)
                    .build();

            Batch savedBatch = batchRepository.save(batch);

            for (Transaction transaction : transactions) {

                transaction.setBatchId(savedBatch.getId());

                transactionRepository.save(transaction);
            }

            return savedBatch;
        }

}
