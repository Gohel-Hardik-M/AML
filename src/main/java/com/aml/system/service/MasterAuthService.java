package com.aml.system.service;

import com.aml.system.dto.auth.LoginResponseDto;
import com.aml.system.dto.auth.MasterLoginRequestDto;
import com.aml.system.exception.AmlBusinessException;
import com.aml.system.multitenancy.TenantContextHolder;
import com.aml.system.security.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.Map;

@Service
public class MasterAuthService {

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
            String sql = "SELECT admin_id, username, password_hash, is_active FROM system_admins WHERE username = ?";
            Map<String, Object> adminData = masterJdbcTemplate.queryForMap(sql, request.getUsername());

            Boolean isActive = (Boolean) adminData.get("is_active");
            if (Boolean.FALSE.equals(isActive)) {
                throw new AmlBusinessException("Master account is deactivated.");
            }

            String storedHash = (String) adminData.get("password_hash");
            if (!passwordEncoder.matches(request.getPassword(), storedHash)) {
                throw new AmlBusinessException("Invalid master admin credentials.");
            }

            String adminId = adminData.get("admin_id").toString();
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