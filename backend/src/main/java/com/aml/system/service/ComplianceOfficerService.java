package com.aml.system.service;

import com.aml.system.dto.admin.ComplianceOfficerCreateDto;
import com.aml.system.dto.admin.ComplianceOfficerResponseDto;
import com.aml.system.exception.AmlBusinessException;
import com.aml.system.model.UserEntity;
import com.aml.system.model.UserRole;
import com.aml.system.multitenancy.TenantContextHolder;
import com.aml.system.repository.AlertRepository;
import com.aml.system.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service for Bank Admin to manage Compliance Officers.
 * Each Bank Admin manages officers strictly within their own tenant database.
 */
@Slf4j
@Service
public class ComplianceOfficerService {

    private static final String ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final AlertRepository alertRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailNotificationService emailService;

    public ComplianceOfficerService(
            UserRepository userRepository,
            AlertRepository alertRepository,
            PasswordEncoder passwordEncoder,
            EmailNotificationService emailService
    ) {
        this.userRepository = userRepository;
        this.alertRepository = alertRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    /**
     * Creates a new Compliance Officer for the logged-in Bank Admin's tenant.
     * Generates a temporary password and emails it to the officer.
     */
    @Transactional
    public ComplianceOfficerResponseDto createComplianceOfficer(ComplianceOfficerCreateDto request) {
        String tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            throw new AmlBusinessException("Tenant context is missing. Cannot create officer.", HttpStatus.BAD_REQUEST);
        }

        // 1. Check if username is already taken in this tenant
        if (userRepository.findByTenantIdAndUsername(tenantId, request.getUsername()).isPresent()) {
            throw new AmlBusinessException(
                    "Username '" + request.getUsername() + "' is already in use for this bank.",
                    HttpStatus.CONFLICT
            );
        }

        // 2. Check if email is already taken
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new AmlBusinessException(
                    "Email '" + request.getEmail() + "' is already in use.",
                    HttpStatus.CONFLICT
            );
        }

        // 3. Generate clean alphanumeric temporary password
        String tempPassword = generateTempPassword(10);

        // 4. Build officer user entity
        UserEntity officer = UserEntity.builder()
                .tenantId(tenantId)
                .username(request.getUsername().trim())
                .email(request.getEmail().trim().toLowerCase())
                .fullName(request.getFullName().trim())
                .passwordHash(passwordEncoder.encode(tempPassword))
                .role(UserRole.COMPLIANCE_OFFICER)
                .isActive(true)
                .isLocked(false)
                .isTemporaryPassword(true)
                .failedAttempts(0)
                .build();

        // 5. Save to tenant database
        UserEntity savedOfficer = userRepository.save(officer);
        log.info("Created compliance officer '{}' for tenant '{}'", savedOfficer.getUsername(), tenantId);

        // 6. Send welcome email with login credentials
        try {
            emailService.sendComplianceOfficerWelcomeEmail(
                    savedOfficer.getEmail(),
                    savedOfficer.getFullName(),
                    tenantId,
                    savedOfficer.getUsername(),
                    tempPassword
            );
        } catch (Exception e) {
            log.warn("Could not send welcome email to compliance officer '{}': {}", savedOfficer.getEmail(), e.getMessage());
        }

        return mapToDto(savedOfficer);
    }

    /**
     * Lists all Compliance Officers for the current tenant.
     */
    @Transactional(readOnly = true)
    public List<ComplianceOfficerResponseDto> getComplianceOfficers() {
        String tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            throw new AmlBusinessException("Tenant context is missing.", HttpStatus.BAD_REQUEST);
        }

        List<UserEntity> officers = userRepository.findByTenantIdAndRole(tenantId, UserRole.COMPLIANCE_OFFICER);
        List<ComplianceOfficerResponseDto> responseList = new ArrayList<>();
        for (UserEntity officer : officers) {
            responseList.add(mapToDto(officer));
        }
        return responseList;
    }

    /**
     * Deactivates a Compliance Officer.
     * When deactivated, all alerts currently assigned to this officer are automatically
     * returned to the unassigned pool (assigned_officer_id = null).
     */
    @Transactional
    public String deactivateComplianceOfficer(UUID officerId) {
        String tenantId = TenantContextHolder.getTenantId();
        UserEntity officer = userRepository.findByTenantIdAndUserId(tenantId, officerId)
                .orElseThrow(() -> new AmlBusinessException("Compliance officer not found.", HttpStatus.NOT_FOUND));

        if (officer.getRole() != UserRole.COMPLIANCE_OFFICER) {
            throw new AmlBusinessException("Selected user is not a compliance officer.", HttpStatus.BAD_REQUEST);
        }

        // Deactivate user
        officer.setIsActive(false);
        userRepository.save(officer);

        // Automatically unassign all alerts assigned to this officer
        int unassignedCount = alertRepository.unassignAlertsByOfficerId(officerId);
        log.info("Deactivated compliance officer '{}'. Returned {} alerts to the unassigned pool.", officer.getUsername(), unassignedCount);

        return "Compliance officer '" + officer.getUsername() + "' has been deactivated. "
                + unassignedCount + " assigned alert(s) have been returned to the unassigned pool.";
    }

    /**
     * Reactivates a previously deactivated Compliance Officer.
     */
    @Transactional
    public String reactivateComplianceOfficer(UUID officerId) {
        String tenantId = TenantContextHolder.getTenantId();
        UserEntity officer = userRepository.findByTenantIdAndUserId(tenantId, officerId)
                .orElseThrow(() -> new AmlBusinessException("Compliance officer not found.", HttpStatus.NOT_FOUND));

        if (officer.getRole() != UserRole.COMPLIANCE_OFFICER) {
            throw new AmlBusinessException("Selected user is not a compliance officer.", HttpStatus.BAD_REQUEST);
        }

        officer.setIsActive(true);
        userRepository.save(officer);
        log.info("Reactivated compliance officer '{}' for tenant '{}'", officer.getUsername(), tenantId);

        return "Compliance officer '" + officer.getUsername() + "' has been reactivated.";
    }

    private ComplianceOfficerResponseDto mapToDto(UserEntity user) {
        return ComplianceOfficerResponseDto.builder()
                .userId(user.getUserId())
                .tenantId(user.getTenantId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .isActive(user.getIsActive())
                .isLocked(user.getIsLocked())
                .createdAt(user.getCreatedAt())
                .build();
    }

    private String generateTempPassword(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHANUMERIC.charAt(RANDOM.nextInt(ALPHANUMERIC.length())));
        }
        return sb.toString();
    }
}
