package com.aml.system.repository;

import com.aml.system.model.Alert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AlertRepository extends JpaRepository<Alert, UUID> {

        // Get all alerts for a specific batch
        List<Alert> findByBatchId(UUID batchId);

        Page<Alert> findByReviewedFalse(Pageable pageable);

        Page<Alert> findByAssignedOfficerId(UUID officerId, Pageable pageable);

        List<Alert> findByBatchIdAndAssignedOfficerId(UUID batchId, UUID officerId);

        @org.springframework.data.jpa.repository.Query("SELECT DISTINCT a.batchId FROM Alert a WHERE a.assignedOfficerId = :officerId AND a.batchId IS NOT NULL")
        List<UUID> findDistinctBatchIdByAssignedOfficerId(
                @org.springframework.data.repository.query.Param("officerId") UUID officerId);

        long countByBatchIdAndReviewedFalse(UUID batchId);

        // Get all alerts that have NOT been reviewed yet
        List<Alert> findByReviewedFalse();

        // Get all alerts assigned to a specific compliance officer
        List<Alert> findByAssignedOfficerId(UUID officerId);

        // Get all alerts that are NOT assigned to anyone (available for assignment)
        List<Alert> findByAssignedOfficerIdIsNull();

        // Unassign all alerts currently assigned to a compliance officer (used when officer is deactivated)
        @org.springframework.data.jpa.repository.Modifying
        @org.springframework.data.jpa.repository.Query("UPDATE Alert a SET a.assignedOfficerId = NULL WHERE a.assignedOfficerId = :officerId")
        int unassignAlertsByOfficerId(@org.springframework.data.repository.query.Param("officerId") UUID officerId);
}
