package com.aml.system.repository;

import com.aml.system.model.CaseEntity;
import com.aml.system.model.enums.CaseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Case Management Repository.
 *
 * ARCHITECTURAL USAGE (JPA):
 * Used for Compliance Officer Dashboard UI, Case Investigation, and Workflow CRUD.
 * JPA provides excellent pagination, dynamic filtering, optimistic locking, and audit integration
 * for low-frequency operational updates.
 */
@Repository
public interface CaseRepository extends JpaRepository<CaseEntity, UUID> {

    Page<CaseEntity> findByTenantIdAndStatus(String tenantId, CaseStatus status, Pageable pageable);

    Page<CaseEntity> findByTenantIdAndAssignedAnalystId(String tenantId, String assignedAnalystId, Pageable pageable);

    Optional<CaseEntity> findByCaseIdAndTenantId(UUID caseId, String tenantId);

    @Query("SELECT COUNT(c) FROM CaseEntity c WHERE c.tenantId = :tenantId AND c.status = :status")
    long countByTenantIdAndStatus(@Param("tenantId") String tenantId, @Param("status") CaseStatus status);
}
