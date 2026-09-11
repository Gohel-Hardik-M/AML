package com.aml.system.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
public class SecurityExceptionHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    // Handles 401 Unauthorized (Missing or Invalid JWT)
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException {
        sendJsonResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized: Invalid or missing JWT Token.");
    }

    // Handles 403 Forbidden (Valid JWT, but lacks required role)
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException {
        sendJsonResponse(response, HttpServletResponse.SC_FORBIDDEN, "Forbidden: You do not have permission to access this resource.");
    }

    private void sendJsonResponse(HttpServletResponse response, int status, String message) throws IOException {
        response.setContentType("application/json");
        response.setStatus(status);
        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(response.getOutputStream(), Map.of(
                "success", false,
                "message", message,
                "errorCode", "SECURITY_ERROR"
        ));
    }
}