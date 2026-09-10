package com.aml.system.multitenancy;

import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TenantRoutingDataSource extends AbstractRoutingDataSource {

    private static final Logger log = LoggerFactory.getLogger(TenantRoutingDataSource.class);

    private final Map<Object, Object> dataSources = new ConcurrentHashMap<>();

    public TenantRoutingDataSource() {

        super.setTargetDataSources(dataSources);
    }

    @Override
    protected Object determineCurrentLookupKey() {
        // This tells Spring which connection to use for the current thread
        return TenantContextHolder.getTenantId();
    }

    /**
     * Checks if a tenant DataSource is registered in the routing map.
     */
    public boolean hasTenant(String tenantId) {
        return tenantId != null && dataSources.containsKey(tenantId);
    }

    /**
     * Registers a new tenant DataSource in the routing map.
     * Synchronized to prevent concurrent modifications during afterPropertiesSet() (audit finding #19).
     * Closes existing DataSource before replacing to prevent connection pool leaks (audit finding #20).
     */
    public synchronized void addTenantDataSource(String tenantId, String url, String username, String password) {
        // Close existing DataSource if replacing (prevents connection pool leak)
        Object existing = dataSources.get(tenantId);
        if (existing instanceof HikariDataSource existingDs) {
            log.info("Closing existing DataSource for tenant '{}' before replacement.", tenantId);
            try {
                existingDs.close();
            } catch (Exception e) {
                log.warn("Failed to close existing DataSource for tenant '{}': {}", tenantId, e.getMessage());
            }
        }

        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        dataSource.setPoolName("HikariPool-" + tenantId);

        // Strict connection limits per tenant to prevent PostgreSQL connection exhaustion
        dataSource.setMaximumPoolSize(10);
        dataSource.setMinimumIdle(2);

        // Connection validation to detect stale connections (audit finding #35)
        dataSource.setConnectionTimeout(30000);   // 30 seconds to get a connection
        dataSource.setIdleTimeout(600000);         // 10 minutes idle before eviction
        dataSource.setMaxLifetime(1800000);        // 30 minutes max lifetime
        dataSource.setConnectionTestQuery("SELECT 1");

        dataSources.put(tenantId, dataSource);

        // Notify Spring that the datasource map has been updated
        this.setTargetDataSources(dataSources);
        this.afterPropertiesSet();

        log.info("DataSource registered for tenant '{}'.", tenantId);
    }


    public synchronized void removeTenantDataSource(String tenantId) {
        Object existing = dataSources.remove(tenantId);

        // Close the connection pool if it exists
        if (existing instanceof HikariDataSource existingDs) {
            try {
                existingDs.close();
                log.info("Closed and removed DataSource for tenant '{}'.", tenantId);
            } catch (Exception e) {
                log.warn("Failed to close DataSource for tenant '{}': {}", tenantId, e.getMessage());
            }
        }

        // Tell Spring the datasource map changed
        this.setTargetDataSources(dataSources);
        this.afterPropertiesSet();
    }
}