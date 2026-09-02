package com.aml.system.service;

import com.aml.system.dto.auth.LoginRequestDto;
import com.aml.system.dto.auth.LoginResponseDto;
import com.aml.system.dto.auth.PasswordResetDto;
import com.aml.system.exception.AmlBusinessException;
import com.aml.system.model.UserEntity;
import com.aml.system.multitenancy.TenantContextHolder;
import com.aml.system.repository.UserRepository;
import com.aml.system.security.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuditLogService auditLogService; // Injected for Sprint 2 Audit Logging

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil, AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public LoginResponseDto login(LoginRequestDto request, HttpServletRequest httpRequest) {
        String tenantId = request.getTenantId();
        String username = request.getUsername();

        // The Controller has already set the database route!
        UserEntity user = userRepository.findByTenantIdAndUsername(tenantId, username)
                .orElseThrow(() -> new AmlBusinessException("Invalid username or password"));

        if (!user.getIsActive()) {
            auditLogService.logAction(username, "LOGIN_FAILED", user.getUserId().toString(), "Account is inactive", httpRequest);
            throw new AmlBusinessException("Account has been deactivated. Please contact your System Administrator.");
        }

        if (user.getIsLocked()) {
            auditLogService.logAction(username, "LOGIN_BLOCKED", user.getUserId().toString(), "Account locked out (5+ failed attempts)", httpRequest);
            throw new AmlBusinessException("Account is locked due to excessive failed attempts. Contact administrator.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            int failed = user.getFailedAttempts() + 1;
            user.setFailedAttempts(failed);

            if (failed >= 5) {
                user.setIsLocked(true);
                userRepository.save(user);
                auditLogService.logAction(username, "ACCOUNT_LOCKED", user.getUserId().toString(), "5 consecutive bad attempts", httpRequest);
                throw new AmlBusinessException("Maximum login attempts exceeded. Account is now locked.");
            }

            userRepository.save(user);
            auditLogService.logAction(username, "LOGIN_FAILED", user.getUserId().toString(), "Invalid password attempt " + failed, httpRequest);
            throw new AmlBusinessException("Invalid username or password");
        }

        user.setFailedAttempts(0);
        userRepository.save(user);

        String token = jwtUtil.generateToken(
                user.getUsername(),
                user.getUserId().toString(),
                user.getTenantId(),
                user.getRole()
        );

        auditLogService.logAction(username, "LOGIN_SUCCESS", user.getUserId().toString(), "Authenticated successfully", httpRequest);

        LoginResponseDto response = new LoginResponseDto();
        response.setToken(token);
        response.setIsTemporaryPassword(user.getIsTemporaryPassword());
        response.setMessage(user.getIsTemporaryPassword()
                ? "Temporary password detected. Password change required."
                : "Authentication successful.");

        return response;
    }
    @Transactional
    public void resetPassword(PasswordResetDto request, HttpServletRequest httpRequest) {
        String tenantId = request.getTenantId();
        String username = request.getUsername();

        try {
            TenantContextHolder.setTenantId(tenantId);

            UserEntity user = userRepository.findByTenantIdAndUsername(tenantId, username)
                    .orElseThrow(() -> new AmlBusinessException("User not found"));

            // Verify current password before allowing change
            if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
                auditLogService.logAction(username, "PWD_RESET_FAIL", user.getUserId().toString(), "Current password mismatch", httpRequest);
                throw new AmlBusinessException("Current password verification failed");
            }

            // Update with new secure hash and clear the temporary/locked flags
            user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
            user.setIsTemporaryPassword(false);
            user.setFailedAttempts(0);
            user.setIsLocked(false);

            userRepository.save(user);

            auditLogService.logAction(username, "PWD_RESET_SUCCESS", user.getUserId().toString(), "Password reset successfully", httpRequest);

        } finally {
            TenantContextHolder.clear();
        }
    }
}