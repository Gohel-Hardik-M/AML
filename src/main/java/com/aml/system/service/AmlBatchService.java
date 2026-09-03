package com.aml.system.service;

import com.aml.system.dto.batch.BatchJobResponseDto;
import com.aml.system.dto.batch.BatchUploadRequestDto;
import com.aml.system.exception.AmlBusinessException;
import com.aml.system.multitenancy.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * High-Scale Batch Ingestion & ELT Pipeline Service.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AmlBatchService {

    private final JobLauncher jobLauncher;
    private final Job amlTransactionBatchJob;
    private final JdbcTemplate jdbcTemplate;

    /**
     * Launches the Spring Batch Chunk-Oriented ETL Pipeline for up to 10,000,000 records.
     */
    public BatchJobResponseDto launchBatchJob(String filePath, BatchUploadRequestDto requestDto) {
        String contextTenantId = TenantContextHolder.getTenantId();
        String requestTenantId = requestDto.getTenantId().trim().toUpperCase(java.util.Locale.ROOT);
        if (contextTenantId != null && !contextTenantId.equalsIgnoreCase(requestTenantId)) {
            throw new AmlBusinessException("Request tenant does not match the authenticated tenant.", org.springframework.http.HttpStatus.FORBIDDEN);
        }
        String tenantId = contextTenantId != null ? contextTenantId : requestTenantId;
        if (tenantId == null || tenantId.isBlank()) {
            throw new AmlBusinessException("Tenant context missing for batch job launch", org.springframework.http.HttpStatus.NOT_FOUND);
        }
        Path inputFile;
        try {
            inputFile = Path.of(filePath).normalize();
        } catch (RuntimeException ex) {
            throw new AmlBusinessException("Batch file path is invalid.", org.springframework.http.HttpStatus.BAD_REQUEST, ex);
        }
        if (!Files.isRegularFile(inputFile) || !Files.isReadable(inputFile)) {
            throw new AmlBusinessException("Batch file does not exist or is not readable.", org.springframework.http.HttpStatus.BAD_REQUEST);
        }

        UUID batchId = UUID.randomUUID();
        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("filePath", filePath)
                    .addString("tenantId", tenantId)
                    .addString("batchId", batchId.toString())
                    .addLong("time", System.currentTimeMillis())
                    .toJobParameters();

            log.info("Triggering Spring Batch Job [amlTransactionBatchJob] for Batch ID: {}, Tenant: {}", batchId, tenantId);
            JobExecution execution = jobLauncher.run(amlTransactionBatchJob, jobParameters);

            return BatchJobResponseDto.builder()
                    .batchId(batchId)
                    .springBatchJobExecutionId(execution.getId())
                    .tenantId(tenantId)
                    .status(execution.getStatus().name())
                    .startTime(LocalDateTime.now())
                    .build();

        } catch (Exception ex) {
            log.error("Failed to launch batch job for batchId {}: {}", batchId, ex.getMessage(), ex);
            throw new AmlBusinessException("Failed to launch Spring Batch execution: " + ex.getMessage(), org.springframework.http.HttpStatus.BAD_REQUEST, ex);
        }
    }

    /**
     * =========================================================================================
     * ARCHITECTURAL PATTERN: DATABASE-NATIVE ELT PIPELINE VIA STORED PROCEDURES
     * =========================================================================================
     * 
     * WHY ELT (EXTRACT - LOAD - TRANSFORM) VIA STORED PROCEDURES IS SUPERIOR FOR 10M DATA WAREHOUSING:
     * 1. ZERO NETWORK HOPPING:
     *    In traditional ETL, 10,000,000 records are pulled over JDBC into Java memory, transformed,
     *    and written back over the wire. This saturates network interfaces (Gigabit NIC) and requires
     *    massive JVM heap allocations.
     * 
     * 2. POSTGRESQL PARALLEL QUERY & VECTORIZED SCANNING:
     *    Executing `CALL process_batch_transactions(?)` lets the PostgreSQL query planner use parallel
     *    workers, partition pruning, bitmap index scans, and SSD sequential I/O directly in the database engine.
     * 
     * 3. TRANSACTIONAL ATOMICITY:
     *    PostgreSQL Stored Procedures allow commit points and idempotent MERGE / UPSERT into core
     *    fact/dimension tables without distributed transaction locks.
     * 
     * USAGE:
     * After CSV records are bulk loaded into `aml_transactions_staging` via PostgreSQL COPY or
     * JdbcTemplate, trigger this method to execute in-database deduplication, account aggregation,
     * and historical baseline scoring.
     * =========================================================================================
     */
    @Async
    @Transactional
    public CompletableFuture<Void> executeEltPipeline(UUID batchId, String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new AmlBusinessException("Tenant context missing for ELT pipeline", org.springframework.http.HttpStatus.NOT_FOUND);
        }
        TenantContextHolder.setTenantId(tenantId);
        log.info("Initiating PostgreSQL in-database ELT procedure CALL process_batch_transactions(?) for batchId: {}, tenant: {}", batchId, tenantId);

        try {
            // PostgreSQL Stored Procedure call
            String sql = "CALL process_batch_transactions(?)";
            jdbcTemplate.update(sql, batchId);

            log.info("Successfully executed PostgreSQL ELT Stored Procedure for batchId: {}", batchId);
            return CompletableFuture.completedFuture(null);

        } catch (Exception ex) {
            log.error("PostgreSQL ELT Stored Procedure execution failed for batchId [{}]: {}", batchId, ex.getMessage(), ex);
            throw new AmlBusinessException(
                    "Database ELT Stored Procedure execution failed: " + ex.getMessage(),
                    org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
                    ex
            );
            } finally {
                TenantContextHolder.clear();
        }
    }
}
