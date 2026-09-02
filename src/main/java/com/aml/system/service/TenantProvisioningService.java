package com.aml.system.service;

import com.aml.system.dto.admin.TenantOnboardRequestDto;
import com.aml.system.model.UserEntity;
import com.aml.system.multitenancy.TenantContextHolder;
import com.aml.system.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

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
        String tenantId = request.getTenantCode().toUpperCase();

        // 1. Create DB, run Flyway, and register the tenant
        databaseService.provisionNewTenantDatabase(tenantId);

        try {
            // 2. Switch context to the brand new database
            TenantContextHolder.setTenantId(tenantId);

            // 3. Generate a secure temporary password
            String tempPassword = "Temp@" + UUID.randomUUID().toString().substring(0, 8) + "!";

            // 4. Seed the initial Bank Admin with Email
            UserEntity admin = UserEntity.builder()
                    .tenantId(tenantId)
                    .username(request.getAdminUsername())
                    .email(request.getAdminEmail()) // <--- Saved from validated DTO
                    .passwordHash(passwordEncoder.encode(tempPassword))
                    .fullName(request.getBankName() + " Admin")
                    .role("TENANT_ADMIN")
                    .isTemporaryPassword(true)
                    .isActive(true)
                    .failedAttempts(0)
                    .isLocked(false)
                    .build();

            userRepository.save(admin);

            // 5. Dispatch email asynchronously (API won't wait for this to finish)
            emailService.sendOnboardingEmail(request.getAdminEmail(), request.getBankName(), tempPassword);

            // 6. Return a generic success message instead of exposing the password
            return "Tenant provisioned successfully. Credentials dispatched via email to " + request.getAdminEmail();

        } finally {
            TenantContextHolder.clear();
        }
    }
}