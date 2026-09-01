package com.aml.system.multitenancy;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

@Service
public class TenantInitializationService {

    private static final Logger log = LoggerFactory.getLogger(TenantInitializationService.class);

    @Autowired
    @Qualifier("masterDataSource")
    private DataSource masterDataSource;

    @Autowired
    @Qualifier("routingDataSource")
    private TenantRoutingDataSource routingDataSource;

    @EventListener(ApplicationReadyEvent.class)
    public void initializeTenantsOnStartup() {
        log.info("=== Starting Multi-Tenant Database Initialization ===");

        // 1. Run Flyway against the Master Database only
        log.info("Migrating Master Database...");
        Flyway masterFlyway = Flyway.configure()
                .dataSource(masterDataSource)
                .locations("classpath:db/migration/master")
                .load();
        masterFlyway.migrate();

        // 2. Fetch all registered banks/tenants from the Master DB
        JdbcTemplate masterJdbcTemplate = new JdbcTemplate(masterDataSource);
        List<Map<String, Object>> tenants = masterJdbcTemplate.queryForList(
                "SELECT tenant_id, db_url, db_username, db_password FROM aml_tenant_registry WHERE is_active = true"
        );

        if (tenants.isEmpty()) {
            log.warn("No active tenants found in the master registry.");
            return;
        }

        // 3. Loop through each tenant, spin up a connection pool, and run their migrations
        for (Map<String, Object> tenant : tenants) {
            String tenantId = (String) tenant.get("tenant_id");
            String url = (String) tenant.get("db_url");
            String username = (String) tenant.get("db_username");
            String password = (String) tenant.get("db_password");

            log.info("Initializing connection and migrating Tenant: {}", tenantId);

            // Add the Hikari connection pool to the router dynamically
            routingDataSource.addTenantDataSource(tenantId, url, username, password);

            // Run Flyway for this specific tenant's database
            Flyway tenantFlyway = Flyway.configure()
                    .dataSource(url, username, password)
                    .locations("classpath:db/migration/tenant")
                    .load();
            tenantFlyway.migrate();
        }

        log.info("=== Multi-Tenant Database Initialization Complete ===");
    }
}