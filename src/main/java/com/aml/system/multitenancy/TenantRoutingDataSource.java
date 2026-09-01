package com.aml.system.multitenancy;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import java.util.HashMap;
import java.util.Map;

public class TenantRoutingDataSource extends AbstractRoutingDataSource {

    private final Map<Object, Object> dataSources = new HashMap<>();

    public TenantRoutingDataSource() {
        // Initialize with the empty map
        super.setTargetDataSources(dataSources);
    }

    @Override
    protected Object determineCurrentLookupKey() {
        // This tells Spring which connection to use for the current thread
        return TenantContextHolder.getTenantId();
    }

    public void addTenantDataSource(String tenantId, String url, String username, String password) {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);

        // Strict connection limits per tenant to prevent PostgreSQL connection exhaustion
        dataSource.setMaximumPoolSize(10);
        dataSource.setMinimumIdle(2);

        dataSources.put(tenantId, dataSource);

        // Notify Spring that the datasource map has been updated
        this.setTargetDataSources(dataSources);
        this.afterPropertiesSet();
    }
}