package com.aml.system.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HexFormat;

/**
 * Manages JWT token blacklisting for logout support.
 * Uses the master database since JWTs are global (not per-tenant).
 * Tokens are stored as SHA-256 hashes to avoid storing raw JWTs.
 */
@Service
public class TokenBlacklistService {

    private static final Logger log = LoggerFactory.getLogger(TokenBlacklistService.class);
    private final JdbcTemplate masterJdbcTemplate;

    public TokenBlacklistService(@Qualifier("masterDataSource") DataSource masterDataSource) {
        this.masterJdbcTemplate = new JdbcTemplate(masterDataSource);
    }

    /**
     * Blacklists a token so it cannot be used again.
     *
     * @param token  The raw JWT string
     * @param expiry When the token would have naturally expired
     */
    public void blacklist(String token, Instant expiry) {
        String hash = sha256(token);
        try {
            masterJdbcTemplate.update(
                    "INSERT INTO blacklisted_tokens (id, token_hash, expiry) VALUES (gen_random_uuid(), ?, ?) ON CONFLICT (token_hash) DO NOTHING",
                    hash, Timestamp.from(expiry)
            );
            log.info("Token blacklisted successfully.");
        } catch (Exception e) {
            log.error("Failed to blacklist token: {}", e.getMessage());
        }
    }

    /**
     * Checks whether a token has been blacklisted (i.e., the user logged out).
     */
    public boolean isBlacklisted(String token) {
        String hash = sha256(token);
        Integer count = masterJdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM blacklisted_tokens WHERE token_hash = ?",
                Integer.class,
                hash
        );
        return count != null && count > 0;
    }

    /**
     * Scheduled cleanup: removes expired blacklist entries every hour.
     * Once a token's natural expiry has passed, keeping it in the blacklist is pointless.
     */
    @Scheduled(fixedRate = 3600000) // 1 hour
    public void cleanupExpiredTokens() {
        int deleted = masterJdbcTemplate.update(
                "DELETE FROM blacklisted_tokens WHERE expiry < ?",
                Timestamp.from(Instant.now())
        );
        if (deleted > 0) {
            log.info("Cleaned up {} expired blacklisted tokens.", deleted);
        }
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
