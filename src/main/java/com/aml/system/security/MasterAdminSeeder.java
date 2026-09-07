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
import java.util.UUID;

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

            // Check if a master admin already exists — DO NOT overwrite (audit finding #5)
            Integer existingCount = masterJdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM system_admins WHERE username = ?",
                    Integer.class,
                    "superadmin"
            );

            if (existingCount != null && existingCount > 0) {
                log.info(">>> Master Superadmin already exists. Skipping seed (password NOT reset).");
                return;
            }

            // First-time setup only: generate a random password and print to console
            String generatedPassword = "Admin@" + UUID.randomUUID().toString().substring(0, 10) + "!";
            String encodedPassword = passwordEncoder.encode(generatedPassword);

            masterJdbcTemplate.update(
                    """
                            INSERT INTO system_admins (admin_id, username, email, password_hash, full_name, is_active)
                            VALUES (gen_random_uuid(), ?, ?, ?, ?, true)
                            ON CONFLICT (username) DO NOTHING
                            """,
                    "superadmin",
                    "admin@aml-platform.com",
                    encodedPassword,
                    "Global System Administrator"
            );

            log.warn("========================================================");
            log.warn("  FIRST-TIME SETUP: Master Admin Created");
            log.warn("  Username : superadmin");
            log.warn("  Password : {}", generatedPassword);
            log.warn("  ** CHANGE THIS PASSWORD IMMEDIATELY **");
            log.warn("========================================================");

        } catch (Exception e) {
            log.warn("Master admin seeding failed: {}", e.getMessage(), e);
        }
    }
}