package com.aml.system.repository;

import com.aml.system.model.Alert;
import com.aml.system.model.enums.AlertSeverity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Alert Management Repository.
 *
 * ARCHITECTURAL USAGE (JPA):
 * Used by UI dashboards to query detected alerts, filter by severity, and bind alerts to Cases.
 */
@Repository
public interface AlertRepository extends JpaRepository<Alert, UUID> {

    Page<Alert> findByTenantId(String tenantId, Pageable pageable);

    Page<Alert> findByTenantIdAndSeverityAndIsReviewed(String tenantId, AlertSeverity severity, Boolean isReviewed, Pageable pageable);

    Page<Alert> findByTenantIdAndIsReviewed(String tenantId, Boolean isReviewed, Pageable pageable);

    Page<Alert> findByTenantIdAndSeverity(String tenantId, AlertSeverity severity, Pageable pageable);

    List<Alert> findByTenantIdAndCustomerId(String tenantId, String customerId);

    Optional<Alert> findByAlertIdAndTenantId(UUID alertId, String tenantId);
}
