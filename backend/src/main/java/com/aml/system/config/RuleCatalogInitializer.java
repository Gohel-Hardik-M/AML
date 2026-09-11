package com.aml.system.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
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

    @Order(2)
    @EventListener(ApplicationReadyEvent.class)
    public void initializeCatalog() {
        log.info("Checking AML Global Rule Catalog initialization on app start...");

        try {
            String insertSql = """
                INSERT INTO aml_global_rule_catalog (typology_name, description, default_thresholds)
                VALUES (?, ?, ?::jsonb)
                ON CONFLICT (typology_name) DO NOTHING
            """;

            masterJdbcTemplate.update(insertSql, "STRUCTURING_001", "Structuring / Smurfing Cash Deposits", "{\"threshold_amount\": 10000}");
            masterJdbcTemplate.update(insertSql, "VELOCITY_001", "Rapid Transaction Velocity Check", "{\"window_minutes\": 60, \"max_count\": 5}");
            masterJdbcTemplate.update(insertSql, "CROSS_BORDER_001", "Cross-Border High Value Transfer", "{\"threshold_amount\": 50000}");
            masterJdbcTemplate.update(insertSql, "CRYPTO_001", "Cryptocurrency Transaction Pattern", "{\"threshold_amount\": 5000}");
            masterJdbcTemplate.update(insertSql, "GEO_RISK_001", "High-Risk Geographic Route", "{\"threshold_amount\": 25000}");
            masterJdbcTemplate.update(insertSql, "SMURFING_001", "Smurfing Layering Network", "{\"threshold_amount\": 3000, \"max_count\": 3, \"window_minutes\": 1440}");

            log.info("AML Global Rule Catalog verified and initialized with 6 core typologies.");
        } catch (Exception e) {
            log.warn("RuleCatalogInitializer notice: {}", e.getMessage());
        }
    }
}
