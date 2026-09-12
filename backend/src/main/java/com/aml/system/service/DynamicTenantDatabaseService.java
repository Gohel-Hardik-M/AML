package com.aml.system.service;

import com.aml.system.exception.AmlBusinessException;
import com.aml.system.multitenancy.TenantRoutingDataSource;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DynamicTenantDatabaseService {

    private static final Logger log = LoggerFactory.getLogger(DynamicTenantDatabaseService.class);

    private final JdbcTemplate masterJdbcTemplate;
    private final TenantRoutingDataSource routingDataSource;
    private final CredentialEncryptionService encryptionService;
    private final Set<String> databasesCreatedByProvisioning = ConcurrentHashMap.newKeySet();

    @Value("${spring.datasource.username}")
    private String dbUsername;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    public DynamicTenantDatabaseService(@Qualifier("masterJdbcTemplate") JdbcTemplate masterJdbcTemplate,
                                       TenantRoutingDataSource routingDataSource,
                                       CredentialEncryptionService encryptionService) {
        this.masterJdbcTemplate = masterJdbcTemplate;
        this.routingDataSource = routingDataSource;
        this.encryptionService = encryptionService;
    }

    /**
     * Provisions a new tenant database: creates DB, runs Flyway, registers in routing, persists to registry.
     * Includes compensation logic for partial failures (audit finding #9).
     */
    public void provisionNewTenantDatabase(String tenantId, String bankName) {
        // Defense-in-depth: re-validate tenantId server-side even though DTO has @Pattern
        if (!tenantId.matches("^[A-Za-z0-9_-]+$")) {
            throw new AmlBusinessException("Invalid tenant ID format", HttpStatus.BAD_REQUEST);
        }

        // Sanitize DB name: strip anything non-alphanumeric/underscore to prevent SQL injection
        String sanitizedTenantId = tenantId.replaceAll("[^a-zA-Z0-9_]", "");
        String dbName = "aml_" + sanitizedTenantId.toLowerCase();
        String safeBankName = (bankName == null || bankName.isBlank()) ? tenantId : bankName;

        // Check if tenant already exists (with ON CONFLICT as backup for TOCTOU race)
        Integer existingTenant = masterJdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM aml_tenant_registry WHERE tenant_id = ?",
            Integer.class,
            tenantId
        );

        if (existingTenant != null && existingTenant > 0) {
            throw new AmlBusinessException(
                "Tenant '" + tenantId + "' already exists.",
                HttpStatus.CONFLICT
            );
        }

        Integer existingDatabase = masterJdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM pg_database WHERE datname = ?",
            Integer.class,
            dbName
        );

        boolean dbCreatedByUs = false;
        boolean routingRegistered = false;
        boolean registryInserted = false;
        String jdbcUrl = "jdbc:postgresql://localhost:5432/" + dbName;

        try {
            // 1. Create the brand new database via Master connection if needed
            // Uses quoted identifier to prevent SQL injection (audit finding #1)
            if (existingDatabase == null || existingDatabase == 0) {
                masterJdbcTemplate.execute("CREATE DATABASE \"" + dbName + "\"");
                dbCreatedByUs = true;
                databasesCreatedByProvisioning.add(tenantId);
                log.info("Created database: {}", dbName);
            }

            // 2. Generate the dynamic connection string
            // 3. Run Flyway automatically on the new database
            Flyway flyway = Flyway.configure()
                    .dataSource(jdbcUrl, dbUsername, dbPassword)
                    .locations("classpath:db/migration/tenant")
                    .baselineOnMigrate(true)
                    .baselineVersion("0")
                    .load();
            flyway.migrate();

            // 4. Inject it into the live router so it works instantly without restarting
            routingDataSource.addTenantDataSource(tenantId, jdbcUrl, dbUsername, dbPassword);
            routingRegistered = true;

            // 5. Encrypt credentials before persisting to registry (audit finding #18)
            String encryptedPassword = encryptionService.encrypt(dbPassword);

            // 6. Persist to Master DB with ON CONFLICT DO NOTHING to handle TOCTOU race (audit finding #2)
            int rowsInserted = masterJdbcTemplate.update(
                "INSERT INTO aml_tenant_registry (tenant_id, bank_name, db_url, db_username, db_password, is_active) " +
                "VALUES (?, ?, ?, ?, ?, true) ON CONFLICT (tenant_id) DO NOTHING",
                tenantId, safeBankName, jdbcUrl, dbUsername, encryptedPassword
            );

            if (rowsInserted == 0) {
                throw new AmlBusinessException(
                        "Tenant '" + tenantId + "' was registered by another request. Please retry.",
                        HttpStatus.CONFLICT
                );
            }
            registryInserted = true;

            log.info("Tenant '{}' provisioned successfully.", tenantId);

        } catch (Exception e) {
            // Compensation: rollback partial state on unexpected failures
            log.error("Tenant provisioning failed for '{}'. Initiating compensation.", tenantId, e);

            // Remove from registry if it was inserted
            if (registryInserted) {
                masterJdbcTemplate.update("DELETE FROM aml_tenant_registry WHERE tenant_id = ? AND db_url = ?",
                    tenantId, jdbcUrl);
            }

            if (routingRegistered) {
                try {
                    routingDataSource.removeTenantDataSource(tenantId);
                } catch (Exception removeEx) {
                    log.error("Compensation: failed to remove datasource for '{}': {}", tenantId, removeEx.getMessage());
                }
            }

            // Drop the database if we created it
            if (dbCreatedByUs) {
                try {
                    masterJdbcTemplate.execute("DROP DATABASE IF EXISTS \"" + dbName + "\"");
                    log.info("Compensation: dropped orphaned database '{}'", dbName);
                } catch (Exception dropEx) {
                    log.error("Compensation: failed to drop database '{}': {}", dbName, dropEx.getMessage());
                }
            }

            if (e instanceof AmlBusinessException businessException) {
                throw businessException;
            }

            throw new AmlBusinessException(
                    "Failed to provision tenant database. All changes have been rolled back.",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    /**
     * Rollback method called when tenant provisioning partially succeeds
     * but email sending fails. Drops the tenant DB and removes the registry entry.
     *
     * This is a "compensation" pattern — we undo what we did since we can't use
     * a single DB transaction across multiple databases.
     */
    public void rollbackTenantProvisioning(String tenantId) {
        String sanitizedTenantId = tenantId.replaceAll("[^a-zA-Z0-9_]", "");
        String dbName = "aml_" + sanitizedTenantId.toLowerCase();

        log.info("Rolling back tenant provisioning for '{}'", tenantId);

        // Step 1: Remove from registry
        try {
            masterJdbcTemplate.update("DELETE FROM aml_tenant_registry WHERE tenant_id = ?", tenantId);
            log.info("Rollback: Removed registry entry for tenant '{}'", tenantId);
        } catch (Exception e) {
            log.error("Rollback: Failed to remove registry entry for '{}': {}", tenantId, e.getMessage());
        }

        // Step 2: Remove from routing datasource
        try {
            routingDataSource.removeTenantDataSource(tenantId);
            log.info("Rollback: Removed datasource for tenant '{}'", tenantId);
        } catch (Exception e) {
            log.error("Rollback: Failed to remove datasource for '{}': {}", tenantId, e.getMessage());
        }

        // Step 3: Drop only a database created by this provisioning operation.
        if (databasesCreatedByProvisioning.remove(tenantId)) {
            try {
                masterJdbcTemplate.execute("DROP DATABASE IF EXISTS \"" + dbName + "\"");
                log.info("Rollback: Dropped database '{}'", dbName);
            } catch (Exception e) {
                log.error("Rollback: Failed to drop database '{}': {}", dbName, e.getMessage());
            }
        } else {
            log.info("Rollback: Preserved pre-existing database '{}'", dbName);
        }
    }
}