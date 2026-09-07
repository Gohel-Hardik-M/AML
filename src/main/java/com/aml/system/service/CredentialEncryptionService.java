package com.aml.system.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * AES-256-GCM encryption service for protecting database credentials at rest.
 * The encryption key must be provided via the AML_ENCRYPTION_KEY environment variable
 * as a Base64-encoded 32-byte key.
 */
@Service
public class CredentialEncryptionService {

    private static final Logger log = LoggerFactory.getLogger(CredentialEncryptionService.class);
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final String ENCRYPTED_PREFIX = "ENC:";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    private final SecretKeySpec secretKey;

    public CredentialEncryptionService(@Value("${aml.encryption.key}") String base64Key) {
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        if (keyBytes.length != 32) {
            throw new IllegalArgumentException("AML encryption key must be exactly 32 bytes (256-bit) Base64-encoded");
        }
        this.secretKey = new SecretKeySpec(keyBytes, "AES");
        log.info("Credential encryption service initialized successfully.");
    }

    /**
     * Encrypts a plaintext string. Returns prefixed ciphertext: "ENC:base64data"
     */
    public String encrypt(String plaintext) {
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            // Combine IV + ciphertext
            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);

            return ENCRYPTED_PREFIX + Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt credential", e);
        }
    }

    /**
     * Decrypts a value. If the value is not prefixed with "ENC:", it is returned as-is
     * (backward compatibility with existing plaintext credentials).
     */
    public String decrypt(String ciphertext) {
        if (ciphertext == null || !ciphertext.startsWith(ENCRYPTED_PREFIX)) {
            // Backward compatible: return plaintext values as-is
            return ciphertext;
        }
        try {
            byte[] combined = Base64.getDecoder().decode(ciphertext.substring(ENCRYPTED_PREFIX.length()));
            byte[] iv = Arrays.copyOfRange(combined, 0, GCM_IV_LENGTH);
            byte[] encrypted = Arrays.copyOfRange(combined, GCM_IV_LENGTH, combined.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt credential", e);
        }
    }
}
