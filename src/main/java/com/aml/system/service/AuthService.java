package com.aml.system.service;

import com.aml.system.domain.User;
import com.aml.system.repository.UserRepository;
import com.aml.system.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private static final int MAX_FAILED_ATTEMPTS = 5;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse authenticate(String email, String rawPassword, String tenantId) {
        // Because TenantContextHolder is already set by the Filter, this query automatically hits the correct DB.
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        if (!user.isActive()) {
            throw new RuntimeException("Account is disabled");
        }
        if (user.isLocked()) {
            throw new RuntimeException("Account is locked due to too many failed attempts. Contact administrator.");
        }

        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            handleFailedAttempt(user);
            throw new RuntimeException("Invalid credentials");
        }

        // Login successful: Reset failed attempts
        user.setFailedAttempts(0);
        userRepository.save(user);

        // Generate Token embedding the tenantId
        String jwtToken = jwtService.generateToken(user, tenantId);

        return new AuthResponse(
                jwtToken,
                user.isTemporaryPassword() // Instructs frontend to force a redirect to the Reset Password screen
        );
    }

    private void handleFailedAttempt(User user) {
        int attempts = user.getFailedAttempts() + 1;
        user.setFailedAttempts(attempts);
        if (attempts >= MAX_FAILED_ATTEMPTS) {
            user.setLocked(true);
        }
        userRepository.save(user);
    }

    @Transactional
    public void resetTemporaryPassword(String email, String newPassword) {
        User user = userRepository.findByEmail(email).orElseThrow();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setTemporaryPassword(false);
        user.setFailedAttempts(0);
        user.setLocked(false);
        userRepository.save(user);
    }
}