package com.aml.system.service;

import com.aml.system.dto.auth.LoginResponseDto;
import com.aml.system.dto.auth.MasterLoginRequestDto;
import com.aml.system.exception.AmlBusinessException;
import com.aml.system.multitenancy.TenantContextHolder;
import com.aml.system.security.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

@Service
public class MasterAuthService {

    private static final Logger log = LoggerFactory.getLogger(MasterAuthService.class);
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long LOCKOUT_DURATION_MINUTES = 30;

    private final JdbcTemplate masterJdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuditLogService auditLogService;

    public MasterAuthService(
            @Qualifier("masterDataSource") DataSource masterDataSource,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil,
            AuditLogService auditLogService) {
        this.masterJdbcTemplate = new JdbcTemplate(masterDataSource);
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.auditLogService = auditLogService;
    }

    public LoginResponseDto masterLogin(MasterLoginRequestDto request, HttpServletRequest httpRequest) {
        TenantContextHolder.clear();

        try {
            String sql = "SELECT admin_id, username, password_hash, is_active, failed_attempts, is_locked, locked_until FROM system_admins WHERE username = ?";
            Map<String, Object> adminData = masterJdbcTemplate.queryForMap(sql, request.getUsername());

            String adminId = adminData.get("admin_id").toString();
            String username = (String) adminData.get("username");

            // Check if account is active
            Boolean isActive = (Boolean) adminData.get("is_active");
            if (Boolean.FALSE.equals(isActive)) {
                throw new AmlBusinessException("Master account is deactivated.");
            }

            // Check lockout with timed auto-unlock
            Boolean isLocked = (Boolean) adminData.get("is_locked");
            Timestamp lockedUntilTs = (Timestamp) adminData.get("locked_until");
            if (Boolean.TRUE.equals(isLocked)) {
                if (lockedUntilTs != null && Instant.now().isAfter(lockedUntilTs.toInstant())) {
                    // Auto-unlock: lockout period expired
                    masterJdbcTemplate.update(
                            "UPDATE system_admins SET is_locked = false, locked_until = NULL, failed_attempts = 0 WHERE admin_id = ?::uuid",
                            adminId
                    );
                    log.info("Master admin account auto-unlocked after {} minutes for user: {}", LOCKOUT_DURATION_MINUTES, username);
                } else {
                    auditLogService.logAction(username, "MASTER_LOGIN_BLOCKED", adminId,
                            "Account locked until " + lockedUntilTs, httpRequest);
                    throw new AmlBusinessException(
                            "Master account is locked due to excessive failed attempts. Try again after 30 minutes.",
                            HttpStatus.FORBIDDEN
                    );
                }
            }

            // Verify password
            String storedHash = (String) adminData.get("password_hash");
            if (!passwordEncoder.matches(request.getPassword(), storedHash)) {
                int currentFailed = adminData.get("failed_attempts") != null
                        ? ((Number) adminData.get("failed_attempts")).intValue() : 0;
                int newFailed = currentFailed + 1;

                if (newFailed >= MAX_FAILED_ATTEMPTS) {
                    Timestamp lockUntil = Timestamp.from(Instant.now().plus(LOCKOUT_DURATION_MINUTES, ChronoUnit.MINUTES));
                    masterJdbcTemplate.update(
                            "UPDATE system_admins SET failed_attempts = ?, is_locked = true, locked_until = ? WHERE admin_id = ?::uuid",
                            newFailed, lockUntil, adminId
                    );
                    auditLogService.logAction(username, "MASTER_ACCOUNT_LOCKED", adminId,
                            MAX_FAILED_ATTEMPTS + " consecutive bad attempts. Locked for " + LOCKOUT_DURATION_MINUTES + " minutes", httpRequest);
                    throw new AmlBusinessException(
                            "Maximum login attempts exceeded. Master account is locked for " + LOCKOUT_DURATION_MINUTES + " minutes.",
                            HttpStatus.FORBIDDEN
                    );
                } else {
                    masterJdbcTemplate.update(
                            "UPDATE system_admins SET failed_attempts = ? WHERE admin_id = ?::uuid",
                            newFailed, adminId
                    );
                    auditLogService.logAction(username, "MASTER_LOGIN_FAILED", adminId,
                            "Invalid password attempt " + newFailed, httpRequest);
                }
                throw new AmlBusinessException("Invalid master admin credentials.");
            }

            // Successful login — reset counters
            masterJdbcTemplate.update(
                    "UPDATE system_admins SET failed_attempts = 0, is_locked = false, locked_until = NULL, last_login = CURRENT_TIMESTAMP WHERE admin_id = ?::uuid",
                    adminId
            );

            String token = jwtUtil.generateToken(
                    request.getUsername(),
                    adminId,
                    "MASTER",
                    "SYSTEM_ADMIN"
            );

            auditLogService.logAction(request.getUsername(), "MASTER_LOGIN_SUCCESS", adminId, "Master Admin Authenticated", httpRequest);

            return LoginResponseDto.builder()
                    .token(token)
                    .isTemporaryPassword(false)
                    .message("Master Administrator authenticated successfully.")
                    .build();

        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            throw new AmlBusinessException("Invalid master admin credentials.");
        }
    }
}