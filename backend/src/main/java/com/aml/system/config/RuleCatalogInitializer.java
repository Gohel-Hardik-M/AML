package com.aml.system.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

@Component
public class RuleCatalogInitializer {

    private static final Logger log = LoggerFactory.getLogger(RuleCatalogInitializer.class);

    private final JdbcTemplate masterJdbcTemplate;

    public RuleCatalogInitializer(@Qualifier("masterDataSource") DataSource masterDataSource) {
        this.masterJdbcTemplate = new JdbcTemplate(masterDataSource);
    }

    @org.springframework.beans.factory.annotation.Value("${aml.multitenancy.auto-init:true}")
    private boolean autoInitEnabled;

    @EventListener(ApplicationReadyEvent.class)
    public void initializeCatalog() {
        if (!autoInitEnabled) {
            log.info("Global Rule Catalog check disabled in current profile/configuration.");
            return;
        }
        log.info("Checking AML Global Rule Catalog initialization on app start...");

        try {
            Integer ruleCount = masterJdbcTemplate.queryForObject(
                    "SELECT count(*) FROM aml_global_rule_catalog WHERE typology_name IN "
                            + "('STRUCTURING_001', 'VELOCITY_001', 'CROSS_BORDER_001', 'GEO_RISK_001', 'SMURFING_001')",
                    Integer.class);
            if (ruleCount == null || ruleCount != 5) {
                throw new IllegalStateException("Required INR-compatible AML rules are missing from the catalog.");
            }
            log.info("AML Global Rule Catalog verified with INR-compatible typologies.");
        } catch (Exception e) {
            throw new IllegalStateException("AML rule catalog initialization failed.", e);
        }
    }
}
