package com.aml.system.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class CredentialEncryptionServiceTest {

    // 32-byte key base64 encoded
    private static final String VALID_KEY_BASE64 = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";
    private CredentialEncryptionService service;

    @BeforeEach
    void setUp() {
        service = new CredentialEncryptionService(VALID_KEY_BASE64);
    }

    @Test
    @DisplayName("Encrypt and decrypt roundtrip restores original plaintext")
    void encryptDecrypt_roundtripRestoresSecret() {
        String secret = "SuperSecretDbPass!#2026";
        String encrypted = service.encrypt(secret);

        assertNotNull(encrypted);
        assertTrue(encrypted.startsWith("ENC:"), "Encrypted value must have ENC: prefix");
        assertNotEquals(secret, encrypted);

        String decrypted = service.decrypt(encrypted);
        assertEquals(secret, decrypted);
    }

    @Test
    @DisplayName("Backward compatibility: Plaintext without ENC: prefix is returned as-is")
    void decrypt_returnsPlaintextWhenNoPrefix() {
        String legacyPlaintext = "postgres_normal_password";
        assertEquals(legacyPlaintext, service.decrypt(legacyPlaintext));
        assertNull(service.decrypt(null));
    }

    @Test
    @DisplayName("Tampered ciphertext fails decryption with exception")
    void decrypt_failsOnTamperedCiphertext() {
        String encrypted = service.encrypt("SensitivePassword123");
        // Tamper with the encrypted portion
        String tampered = encrypted.substring(0, encrypted.length() - 4) + "AAAA";
        assertThrows(RuntimeException.class, () -> service.decrypt(tampered));
    }

    @Test
    @DisplayName("Constructor rejects keys that are not 32 bytes (256-bit)")
    void constructor_rejectsInvalidKeyLength() {
        // Key with only 16 bytes
        String shortKey = Base64.getEncoder().encodeToString("1234567890123456".getBytes());
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new CredentialEncryptionService(shortKey)
        );
        assertTrue(exception.getMessage().contains("must be exactly 32 bytes"));
    }
}
