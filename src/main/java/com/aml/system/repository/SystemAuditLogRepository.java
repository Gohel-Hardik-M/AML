package com.aml.system.repository;

import com.aml.system.model.SystemAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SystemAuditLogRepository extends JpaRepository<SystemAuditLog, UUID> {

    /**
     * Paginated audit log retrieval for compliance review.
     */
    Page<SystemAuditLog> findByUserId(String userId, Pageable pageable);

    Page<SystemAuditLog> findByActionType(String actionType, Pageable pageable);
}