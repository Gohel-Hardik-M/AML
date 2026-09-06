package com.aml.system.repository;

import com.aml.system.model.BatchEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BatchRepository extends JpaRepository<BatchEntity, UUID> {

    // Core business rule: Same tenant + same date = same business batch
    Optional<BatchEntity> findByTenantIdAndBatchDate(String tenantId, LocalDate batchDate);

    // Prevents uploading the exact same identical file twice
    boolean existsByTenantIdAndFileChecksum(String tenantId, String fileChecksum);

    // Fetch all batches for a specific tenant (for the GET /api/v1/batches endpoint)
    List<BatchEntity> findAllByTenantIdOrderByBatchDateDesc(String tenantId);
}