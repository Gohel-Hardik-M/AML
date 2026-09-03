package com.aml.system.multitenancy;

import com.aml.system.model.UserEntity;
import com.aml.system.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class TenantDataSeeder {

    private static final Logger log = LoggerFactory.getLogger(TenantDataSeeder.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate masterJdbcTemplate;

    // Inject the masterDataSource so we can read the central tenant registry
    public TenantDataSeeder(UserRepository userRepository,
                            PasswordEncoder passwordEncoder,
                            @Qualifier("masterDataSource") DataSource masterDataSource) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.masterJdbcTemplate = new JdbcTemplate(masterDataSource);
    }

    @Order(2)
    @EventListener(ApplicationReadyEvent.class)
    public void seedAdminUsers() {

        // 1. Dynamically fetch all active tenants from the master registry
        List<Map<String, Object>> tenants = masterJdbcTemplate.queryForList(
                "SELECT tenant_id FROM aml_tenant_registry WHERE is_active = true"
        );

        if (tenants.isEmpty()) {
            log.info("No active tenants found for seeding.");
            return;
        }

        // 2. Loop through every single bank in the system
        for (Map<String, Object> tenantRow : tenants) {
            String tenantId = (String) tenantRow.get("tenant_id");
            String adminUsername = "admin_" + tenantId.toLowerCase();

            try {
                // 3. Switch connection to this specific tenant's database
                TenantContextHolder.setTenantId(tenantId);

                Optional<UserEntity> existingAdmin = userRepository.findByTenantIdAndUsername(tenantId, adminUsername);

                if (existingAdmin.isEmpty()) {
                    log.info("Seeding default Bank Admin for tenant: {}", tenantId);

                    UserEntity admin = UserEntity.builder()
                            .tenantId(tenantId)
                            .username(adminUsername)
                            .passwordHash(passwordEncoder.encode("admin123"))
                            .fullName(tenantId + " Administrator")
                            .role("TENANT_ADMIN")
                            .isActive(true)
                            .isLocked(false)
                            .failedAttempts(0)
                            .isTemporaryPassword(true)
                            .build();

                    try {
                        userRepository.save(admin);
                        log.info("Bank Admin created successfully for {}!", tenantId);
                    } catch (Exception saveEx) {
                        log.warn("Admin already exists or could not be seeded for tenant {}: {}",
                                tenantId, saveEx.getMessage());
                    }
                } else {
                    log.info("Admin already exists for tenant: {}. Skipping seed.", tenantId);
                }

            } catch (Exception e) {
                log.error("Failed to seed admin user for tenant {}: {}", tenantId, e.getMessage());
            } finally {
                // 4. Always clear the routing context before the next loop iteration!
                TenantContextHolder.clear();
            }
        }
    }
}