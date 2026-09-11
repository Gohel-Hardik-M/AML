package com.aml.system.service;

import com.aml.system.dto.admin.TenantOnboardRequestDto;
import com.aml.system.dto.admin.TenantSummaryDto;
import com.aml.system.exception.AmlBusinessException;
import com.aml.system.model.UserEntity;
import com.aml.system.model.UserRole;
import com.aml.system.multitenancy.TenantContextHolder;
import com.aml.system.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.List;
import java.util.Locale;

@Service
public class TenantProvisioningService {

    private static final Logger log = LoggerFactory.getLogger(TenantProvisioningService.class);

    private final DynamicTenantDatabaseService databaseService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailNotificationService emailService;
    private final JdbcTemplate masterJdbcTemplate;

    // Characters for temp password — only letters and numbers (no special chars)
    // This prevents copy-paste issues from email clients
    private static final String PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
    private static final int PASSWORD_LENGTH = 12;
    private static final SecureRandom RANDOM = new SecureRandom();

    public TenantProvisioningService(
            DynamicTenantDatabaseService databaseService,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            EmailNotificationService emailService,
            @Qualifier("masterJdbcTemplate") JdbcTemplate masterJdbcTemplate) {
        this.databaseService = databaseService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.masterJdbcTemplate = masterJdbcTemplate;
    }

        public List<TenantSummaryDto> listTenants() {
        return masterJdbcTemplate.query(
            "SELECT tenant_id, bank_name, is_active, created_at " +
                "FROM aml_tenant_registry ORDER BY created_at DESC",
            (resultSet, rowNum) -> new TenantSummaryDto(
                resultSet.getString("tenant_id"),
                resultSet.getString("bank_name"),
                resultSet.getBoolean("is_active"),
                resultSet.getTimestamp("created_at") == null
                    ? null
                    : resultSet.getTimestamp("created_at").toInstant()
            )
        );
        }

    /**
     * Onboards a new bank tenant.
     *
     * Steps:
     * 1. Create DB + run Flyway migrations
     * 2. Create the Bank Admin user
     * 3. Send email with temp password (SYNCHRONOUS — if this fails, everything rolls back)
     *
     * If email fails → admin user is deleted → tenant DB is dropped → registry entry removed.
     */
    public String onboardNewBank(TenantOnboardRequestDto request) {
        String tenantId = request.getTenantCode().trim().toUpperCase(Locale.ROOT);
        String bankName = request.getBankName().trim();
        String adminUsername = request.getAdminUsername().trim();
        String adminEmail = request.getAdminEmail().trim().toLowerCase(Locale.ROOT);

        // Step 1: Create DB, run Flyway, and register the tenant
        databaseService.provisionNewTenantDatabase(tenantId, bankName);

        try {
            // Step 2: Switch to the new tenant's database
            TenantContextHolder.setTenantId(tenantId);

            // Step 3: Generate a clean temp password (letters + numbers only)
            String tempPassword = generateTempPassword();

            // Step 4: Create the Bank Admin user
            UserEntity admin = UserEntity.builder()
                    .tenantId(tenantId)
                    .username(adminUsername)
                    .email(adminEmail)
                    .passwordHash(passwordEncoder.encode(tempPassword))
                    .fullName(bankName + " Admin")
                    .role(UserRole.TENANT_ADMIN)
                    .isTemporaryPassword(true)
                    .isActive(true)
                    .failedAttempts(0)
                    .isLocked(false)
                    .build();

            try {
                userRepository.save(admin);
            } catch (Exception e) {
                if (e.getMessage() != null && e.getMessage().toLowerCase().contains("username")) {
                    throw new AmlBusinessException(
                            "Admin username '" + adminUsername + "' already exists for this tenant.",
                            HttpStatus.CONFLICT
                    );
                }
                if (e.getMessage() != null && e.getMessage().toLowerCase().contains("email")) {
                    throw new AmlBusinessException(
                            "Admin email '" + adminEmail + "' already exists.",
                            HttpStatus.CONFLICT
                    );
                }
                throw new AmlBusinessException("Unable to create tenant administrator.", HttpStatus.INTERNAL_SERVER_ERROR);
            }

            // Step 5: Send email SYNCHRONOUSLY — if this fails, we rollback everything
            try {
                emailService.sendOnboardingEmail(adminEmail, bankName, tempPassword);
            } catch (Exception emailError) {
                // Email failed! We need to clean up everything.
                log.error("Email failed for tenant '{}'. Rolling back entire provisioning.", tenantId, emailError);

                // Delete the admin user we just created
                try {
                    userRepository.delete(admin);
                    log.info("Rollback: Deleted admin user for tenant '{}'", tenantId);
                } catch (Exception deleteError) {
                    log.error("Rollback: Failed to delete admin user: {}", deleteError.getMessage());
                }

                // Clear tenant context before dropping
                TenantContextHolder.clear();

                // Drop the tenant database and registry entry
                databaseService.rollbackTenantProvisioning(tenantId);

                throw new AmlBusinessException(
                        "Tenant provisioning failed: Could not send credentials email to " + adminEmail +
                        ". All changes have been rolled back. Please verify the email address and try again.",
                        HttpStatus.INTERNAL_SERVER_ERROR
                );
            }

            // Step 6: Everything worked! Return success message.
            return "Tenant provisioned successfully. Credentials have been dispatched via email.";

        } finally {
            TenantContextHolder.clear();
        }
    }

    /**
     * Generates a temporary password using only letters and numbers.
     * No special characters — prevents copy-paste issues from email clients.
     *
     * Example output: "TmPx7Kn3Rq2W"
     */
    public static String generateTempPassword() {
        StringBuilder password = new StringBuilder(PASSWORD_LENGTH);
        for (int i = 0; i < PASSWORD_LENGTH; i++) {
            int index = RANDOM.nextInt(PASSWORD_CHARS.length());
            password.append(PASSWORD_CHARS.charAt(index));
        }
        return password.toString();
    }
}