package com.aml.system.security;

import com.aml.system.multitenancy.TenantContextHolder;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
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

            // 2. Validate the token
            if (jwtUtil.isTokenValid(jwt) && SecurityContextHolder.getContext().getAuthentication() == null) {

                Claims claims = jwtUtil.extractAllClaims(jwt);
                String username = claims.getSubject();
                String tenantId = claims.get("tenantId", String.class);
                String role = claims.get("role", String.class);

                // --- NEW CODE: Ensure Spring Security recognizes this as a Role ---
                if (role != null && !role.startsWith("ROLE_")) {
                    role = "ROLE_" + role;
                }

                // 3. CRITICAL: Set the database routing context for this specific request!
                if (tenantId != null) {
                    TenantContextHolder.setTenantId(tenantId);
                }

                // 4. Tell Spring Security who this user is and what their role is
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        username,
                        null,
                        Collections.singletonList(new SimpleGrantedAuthority(role))
                );

                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }

            // 5. Continue processing the request
            filterChain.doFilter(request, response);

        } finally {
            // 6. ALWAYS clear the context after the request finishes to prevent data leaks!
            TenantContextHolder.clear();
        }
    }
}