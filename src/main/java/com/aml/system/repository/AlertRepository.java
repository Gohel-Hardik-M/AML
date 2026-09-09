package com.aml.system.repository;

import com.aml.system.model.Alert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AlertRepository extends JpaRepository<Alert, UUID> {

        List<Alert> findByBatchId(UUID batchId);

        List<Alert> findByReviewedFalse();


}
