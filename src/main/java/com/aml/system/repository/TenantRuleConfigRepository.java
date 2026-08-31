package com.aml.system.repository;

import com.aml.system.model.TenantRuleConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantRuleConfigRepository extends JpaRepository<TenantRuleConfig, UUID> {

    List<TenantRuleConfig> findByTenantIdAndIsEnabledTrue(String tenantId);

    Optional<TenantRuleConfig> findByTenantIdAndRuleCode(String tenantId, String ruleCode);
}
