package com.aml.system.service;

import com.aml.system.dto.admin.TenantOnboardRequestDto;
import com.aml.system.exception.AmlBusinessException;
import com.aml.system.model.UserEntity;
import com.aml.system.multitenancy.TenantContextHolder;
import com.aml.system.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.Locale;

@Service
public class TenantProvisioningService {

    private final DynamicTenantDatabaseService databaseService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailNotificationService emailService;

    public TenantProvisioningService(
            DynamicTenantDatabaseService databaseService,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            EmailNotificationService emailService) {
        this.databaseService = databaseService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    public String onboardNewBank(TenantOnboardRequestDto request) {
        String tenantId = request.getTenantCode().trim().toUpperCase(Locale.ROOT);
        String bankName = request.getBankName().trim();
        String adminUsername = request.getAdminUsername().trim();
        String adminEmail = request.getAdminEmail().trim().toLowerCase(Locale.ROOT);

        // 1. Create DB, run Flyway, and register the tenant
        databaseService.provisionNewTenantDatabase(tenantId, bankName);

        try {
            // 2. Switch context to the brand new database
            TenantContextHolder.setTenantId(tenantId);

            // 3. Generate a secure temporary password
            String tempPassword = "Temp@" + UUID.randomUUID().toString().substring(0, 8) + "!";

            // 4. Seed the initial Bank Admin with Email
            UserEntity admin = UserEntity.builder()
                    .tenantId(tenantId)
                    .username(adminUsername)
                    .email(adminEmail)
                    .passwordHash(passwordEncoder.encode(tempPassword))
                    .fullName(bankName + " Admin")
                    .role("TENANT_ADMIN")
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

            // 5. Dispatch email asynchronously (API won't wait for the SMTP result)
            emailService.sendOnboardingEmail(adminEmail, bankName, tempPassword);

            // 6. Return a generic success message instead of exposing the password
            return "Tenant provisioned successfully. Credentials dispatched via email to " + adminEmail;

        } finally {
            TenantContextHolder.clear();
        }
    }
}