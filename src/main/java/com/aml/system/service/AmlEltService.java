package com.aml.system.service;

import com.aml.system.exception.AmlBusinessException;
import com.aml.system.multitenancy.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class AmlEltService {

    private final JdbcTemplate jdbcTemplate;

    @Async
    @Transactional
    public CompletableFuture<Void> executeEltPipeline(UUID batchId, String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new AmlBusinessException("Tenant context missing for ELT pipeline", HttpStatus.NOT_FOUND);
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
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    ex
            );
        } finally {
            TenantContextHolder.clear();
        }
    }
}