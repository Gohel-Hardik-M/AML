package com.aml.system.service;

import com.aml.system.exception.AmlBusinessException;
import com.aml.system.exception.ErrorCodeEnum;
import com.aml.system.multitenancy.TenantRoutingDataSource;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class DynamicTenantDatabaseService {

    private final JdbcTemplate masterJdbcTemplate;
    private final TenantRoutingDataSource routingDataSource;

    @Value("${spring.datasource.username}")
    private String dbUsername;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    public DynamicTenantDatabaseService(JdbcTemplate masterJdbcTemplate, TenantRoutingDataSource routingDataSource) {
        this.masterJdbcTemplate = masterJdbcTemplate;
        this.routingDataSource = routingDataSource;
    }

    public void provisionNewTenantDatabase(String tenantId) {
        String dbName = "aml_" + tenantId.toLowerCase();

        Integer existingTenant = masterJdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM aml_tenant_registry WHERE tenant_id = ?",
            Integer.class,
            tenantId
        );

        if (existingTenant != null && existingTenant > 0) {
            throw new AmlBusinessException(
                ErrorCodeEnum.BATCH_DUPLICATE_INGESTION,
                "Tenant already exists: " + tenantId
            );
        }

        Integer existingDatabase = masterJdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM pg_database WHERE datname = ?",
            Integer.class,
            dbName
        );

        // 1. Create the brand new database via Master connection if needed
        if (existingDatabase == null || existingDatabase == 0) {
            masterJdbcTemplate.execute("CREATE DATABASE " + dbName);
        }

        // 2. Generate the dynamic connection string
        String jdbcUrl = "jdbc:postgresql://localhost:5432/" + dbName;

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

        // 5. Permanently save to Master DB so TenantInitializationService finds it on next reboot
        masterJdbcTemplate.update(
                "INSERT INTO aml_tenant_registry (tenant_id, db_url, db_username, db_password, is_active) VALUES (?, ?, ?, ?, true)",
                tenantId, jdbcUrl, dbUsername, dbPassword
        );
    }
}