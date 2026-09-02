package com.aml.system.security;

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

@Component
public class MasterAdminSeeder {

    private static final Logger log = LoggerFactory.getLogger(MasterAdminSeeder.class);
    private final JdbcTemplate masterJdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    public MasterAdminSeeder(
            @Qualifier("masterDataSource") DataSource masterDataSource,
            PasswordEncoder passwordEncoder) {
        this.masterJdbcTemplate = new JdbcTemplate(masterDataSource);
        this.passwordEncoder = passwordEncoder;
    }

    @Order(3)
    @EventListener(ApplicationReadyEvent.class)
    public void seedMasterAdmin() {
        try {
            Integer tableExists = masterJdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'system_admins'",
                    Integer.class
            );

            if (tableExists == null || tableExists == 0) {
                log.warn("system_admins table does not exist yet. Skipping master admin seed.");
                return; // Flyway hasn't created the table yet, skip for now
            }

            String encodedPassword = passwordEncoder.encode("admin123");

            masterJdbcTemplate.update(
                    """
                            INSERT INTO system_admins (admin_id, username, email, password_hash, full_name, is_active)
                            VALUES (gen_random_uuid(), ?, ?, ?, ?, true)
                            ON CONFLICT (username) DO UPDATE
                            SET password_hash = EXCLUDED.password_hash,
                                email = EXCLUDED.email,
                                full_name = EXCLUDED.full_name,
                                is_active = true
                            """,
                    "superadmin",
                    "admin@aml-platform.com",
                    encodedPassword,
                    "Global System Administrator"
            );

            log.info(">>> SUCCESS: Master Superadmin seeded or refreshed successfully.");
        } catch (Exception e) {
            log.warn("Master admin seeding failed: {}", e.getMessage(), e);
        }
    }
}