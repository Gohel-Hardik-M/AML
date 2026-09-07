package com.aml.system.service;

import com.aml.system.dto.auth.LoginRequestDto;
import com.aml.system.dto.auth.LoginResponseDto;
import com.aml.system.dto.auth.PasswordResetDto;
import com.aml.system.exception.AmlBusinessException;
import com.aml.system.model.UserEntity;
import com.aml.system.repository.UserRepository;
import com.aml.system.security.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long LOCKOUT_DURATION_MINUTES = 30;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuditLogService auditLogService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil, AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.auditLogService = auditLogService;
    }

    /**
     * Authenticates a tenant user with pessimistic locking to prevent
     * race conditions on the failed_attempts counter (audit finding #3).
     */
    @Transactional
    public LoginResponseDto login(LoginRequestDto request, HttpServletRequest httpRequest) {
        String tenantId = request.getTenantId();
        String username = request.getUsername();

        // Uses PESSIMISTIC_WRITE lock to prevent concurrent login attempts
        // from causing lost updates on failed_attempts counter.
        UserEntity user = userRepository.findByTenantIdAndUsernameForUpdate(tenantId, username)
                .orElseThrow(() -> new AmlBusinessException("Invalid username or password"));

        if (!user.getIsActive()) {
            auditLogService.logAction(username, "LOGIN_FAILED", user.getUserId().toString(), "Account is inactive", httpRequest);
            throw new AmlBusinessException("Account has been deactivated. Please contact your System Administrator.");
        }

        // Timed lockout: check if the lockout period has expired
        if (user.getIsLocked()) {
            if (user.getLockedUntil() != null && Instant.now().isAfter(user.getLockedUntil())) {
                // Auto-unlock: lockout period has expired
                user.setIsLocked(false);
                user.setLockedUntil(null);
                user.setFailedAttempts(0);
                userRepository.save(user);
                log.info("Account auto-unlocked after {} minutes for user: {}", LOCKOUT_DURATION_MINUTES, username);
            } else {
                auditLogService.logAction(username, "LOGIN_BLOCKED", user.getUserId().toString(),
                        "Account locked until " + user.getLockedUntil(), httpRequest);
                throw new AmlBusinessException(
                        "Account is locked due to excessive failed attempts. Try again after 30 minutes or contact administrator.",
                        HttpStatus.FORBIDDEN
                );
            }
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            int failed = user.getFailedAttempts() + 1;
            user.setFailedAttempts(failed);

            if (failed >= MAX_FAILED_ATTEMPTS) {
                user.setIsLocked(true);
                user.setLockedUntil(Instant.now().plus(LOCKOUT_DURATION_MINUTES, ChronoUnit.MINUTES));
                userRepository.save(user);
                auditLogService.logAction(username, "ACCOUNT_LOCKED", user.getUserId().toString(),
                        MAX_FAILED_ATTEMPTS + " consecutive bad attempts. Locked for " + LOCKOUT_DURATION_MINUTES + " minutes", httpRequest);
                throw new AmlBusinessException(
                        "Maximum login attempts exceeded. Account is locked for " + LOCKOUT_DURATION_MINUTES + " minutes.",
                        HttpStatus.FORBIDDEN
                );
            }

            userRepository.save(user);
            auditLogService.logAction(username, "LOGIN_FAILED", user.getUserId().toString(), "Invalid password attempt " + failed, httpRequest);
            throw new AmlBusinessException("Invalid username or password");
        }

        // Successful login — reset counters
        user.setFailedAttempts(0);
        user.setIsLocked(false);
        user.setLockedUntil(null);
        userRepository.save(user);

        String token = jwtUtil.generateToken(
                user.getUsername(),
                user.getUserId().toString(),
                user.getTenantId(),
                user.getRole().name()
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

    /**
     * Resets a user's password. Requires knowing the current password.
     * TenantContextHolder is set by the controller/filter — NOT duplicated here (audit finding #30).
     */
    @Transactional
    public void resetPassword(PasswordResetDto request, HttpServletRequest httpRequest) {
        String tenantId = request.getTenantId();
        String username = request.getUsername();

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
        user.setLockedUntil(null);

        userRepository.save(user);

        auditLogService.logAction(username, "PWD_RESET_SUCCESS", user.getUserId().toString(), "Password reset successfully", httpRequest);
    }
}