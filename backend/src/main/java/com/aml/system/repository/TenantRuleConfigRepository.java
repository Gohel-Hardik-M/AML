package com.aml.system.repository;

import com.aml.system.model.TenantRuleConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantRuleConfigRepository extends JpaRepository<TenantRuleConfig, UUID> {

    // Get all rule configs for a specific tenant
    List<TenantRuleConfig> findByTenantId(String tenantId);

    // Get a specific rule config for a tenant + rule code combo
    Optional<TenantRuleConfig> findByTenantIdAndRuleCode(String tenantId, String ruleCode);
}
