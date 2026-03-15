package com.wwh.simplepic.security;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Secure token generator
 * 安全令牌生成器
 */
public class SecureTokenGenerator {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int DEFAULT_TOKEN_LENGTH = 32;

    /**
     * Generate a cryptographically secure random token
     *
     * @return Base64-encoded random token
     */
    public static String generateToken() {
        return generateToken(DEFAULT_TOKEN_LENGTH);
    }

    /**
     * Generate a cryptographically secure random token with specified length
     *
     * @param length number of random bytes (before Base64 encoding)
     * @return Base64-encoded random token
     */
    public static String generateToken(int length) {
        byte[] randomBytes = new byte[length];
        SECURE_RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    /**
     * Generate a random string for API keys
     *
     * @return random string
     */
    public static String generateApiKeyToken() {
        // Generate a longer token for API keys
        byte[] randomBytes = new byte[48];
        SECURE_RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}