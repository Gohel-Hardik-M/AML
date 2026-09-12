package com.aml.system.security;

import com.aml.system.model.UserEntity;
import com.aml.system.multitenancy.TenantContextHolder;
import com.aml.system.repository.UserRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final JdbcTemplate masterJdbcTemplate;

    public JwtAuthenticationFilter(JwtUtil jwtUtil,
                                   UserRepository userRepository,
                                   @Qualifier("masterDataSource") DataSource masterDataSource) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.masterJdbcTemplate = new JdbcTemplate(masterDataSource);
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        // 1. If there is no token, continue the filter chain (Spring Security will block it later if required)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            final String jwt = authHeader.substring(7);

            // 2. Validate the token signature and expiry
            if (jwtUtil.isTokenValid(jwt) && SecurityContextHolder.getContext().getAuthentication() == null) {

                Claims claims = jwtUtil.extractAllClaims(jwt);
                String username = claims.getSubject();
                String tenantId = claims.get("tenantId", String.class);

                // Route user lookups to the database identified by the verified JWT.
                if (tenantId != null) {
                    TenantContextHolder.setTenantId(tenantId);
                }

                String role = currentRole(tenantId, username);

                // Ensure Spring Security recognizes this as a Role
                if (role != null && !role.startsWith("ROLE_")) {
                    role = "ROLE_" + role;
                }

                // 4. Check if user is still active and not locked (audit finding #24)
                if (!isUserStillValid(tenantId, username)) {
                    log.warn("JWT valid but user '{}' is inactive/locked. Rejecting request.", username);
                    TenantContextHolder.clear();
                    filterChain.doFilter(request, response);
                    return;
                }

                // 5. Check if user has temporary password — block all endpoints except reset-password
                String requestPath = request.getRequestURI();
                if (isTemporaryPasswordUser(tenantId, username)
                        && !requestPath.contains("/reset-password")) {
                    log.warn("User '{}' has temporary password. Blocking access to '{}'.", username, requestPath);
                    response.setContentType("application/json");
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.getWriter().write("{\"success\":false,\"message\":\"Password change required. Please reset your temporary password before accessing the system.\",\"errorCode\":\"TEMP_PASSWORD\"}");
                    return;
                }

                // 6. Tell Spring Security who this user is and what their role is
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        username,
                        null,
                        Collections.singletonList(new SimpleGrantedAuthority(role))
                );

                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }

            // 7. Continue processing the request
            filterChain.doFilter(request, response);

        } finally {
            // 8. ALWAYS clear the context after the request finishes to prevent data leaks!
            TenantContextHolder.clear();
        }
    }

    /**
     * Checks if the user is still active and not locked.
     * For MASTER tenant, checks system_admins. For others, checks aml_users.
     */
    private boolean isUserStillValid(String tenantId, String username) {
        try {
            if ("MASTER".equals(tenantId)) {
                Map<String, Object> admin = masterJdbcTemplate.queryForMap(
                        "SELECT is_active, is_locked FROM system_admins WHERE username = ?", username
                );
                Boolean isActive = (Boolean) admin.get("is_active");
                Boolean isLocked = (Boolean) admin.get("is_locked");
                return Boolean.TRUE.equals(isActive) && !Boolean.TRUE.equals(isLocked);
            } else {
                Optional<UserEntity> userOpt = userRepository.findByTenantIdAndUsername(tenantId, username);
                if (userOpt.isEmpty()) return false;
                UserEntity user = userOpt.get();
                return user.getIsActive() && !user.getIsLocked();
            }
        } catch (Exception e) {
            log.error("Failed to validate user status for '{}': {}", username, e.getMessage());
            return false; // Fail closed: if we can't verify, reject
        }
    }

    /**
     * Checks if the user still has a temporary password that needs to be changed.
     */
    private boolean isTemporaryPasswordUser(String tenantId, String username) {
        try {
            if ("MASTER".equals(tenantId)) {
                return false; // Master admins don't have temporary password flow
            }
            Optional<UserEntity> userOpt = userRepository.findByTenantIdAndUsername(tenantId, username);
            return userOpt.isPresent() && userOpt.get().getIsTemporaryPassword();
        } catch (Exception e) {
            log.error("Failed to check temp password for '{}': {}", username, e.getMessage());
            return false;
        }
    }

    private String currentRole(String tenantId, String username) {
        if ("MASTER".equals(tenantId)) {
            return "SYSTEM_ADMIN";
        }
        return userRepository.findByTenantIdAndUsername(tenantId, username)
                .map(user -> user.getRole().name())
                .orElse(null);
    }
}