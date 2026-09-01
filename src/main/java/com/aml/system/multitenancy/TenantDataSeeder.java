package com.aml.system.multitenancy;

import com.aml.system.model.UserEntity;
import com.aml.system.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class TenantDataSeeder {

    private static final Logger log = LoggerFactory.getLogger(TenantDataSeeder.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public TenantDataSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // @Order(2) ensures this runs AFTER your TenantInitializationService (which should be @Order(1))
    @Order(2)
    @EventListener(ApplicationReadyEvent.class)
    public void seedAdminUsers() {
        // For Sprint 1, we are hardcoding the seed for HDFC.
        // Later, this will loop through all tenants dynamically.
        String tenantId = "HDFC";
        String adminUsername = "admin_hdfc";

        try {
            // 1. Switch connection to the HDFC database
            TenantContextHolder.setTenantId(tenantId);

            // 2. Check if the admin already exists to prevent duplicate entries
            Optional<UserEntity> existingAdmin = userRepository.findByTenantIdAndUsername(tenantId, adminUsername);

            if (existingAdmin.isEmpty()) {
                log.info("Seeding default System Admin for tenant: {}", tenantId);

                UserEntity admin = UserEntity.builder()
                        .tenantId(tenantId)
                        .username(adminUsername)
                        .passwordHash(passwordEncoder.encode("admin123")) // Secure BCrypt Hash
                        .fullName("HDFC System Administrator")
                        .role("TENANT_ADMIN")
                        .isActive(true)
                        .build();

                userRepository.save(admin);
                log.info("System Admin created successfully!");
            } else {
                log.info("Admin already exists for tenant: {}. Skipping seed.", tenantId);
            }

        } catch (Exception e) {
            log.error("Failed to seed admin user for tenant {}: {}", tenantId, e.getMessage());
        } finally {
            // 3. Always clear the routing context!
            TenantContextHolder.clear();
        }
    }
}