package com.wwh.simplepic.security;

import java.security.SecureRandom;

/**
 * Secure token generator
 * 安全令牌生成器
 */
public class SecureTokenGenerator {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int DEFAULT_TOKEN_LENGTH = 32;

    /**
     * Character set for alphanumeric tokens (letters and numbers only)
     * 不包含特殊字符的字符集（仅字母和数字）
     */
    private static final String ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    /**
     * Generate a cryptographically secure random token
     *
     * @return alphanumeric random token
     */
    public static String generateToken() {
        return generateToken(DEFAULT_TOKEN_LENGTH);
    }

    /**
     * Generate a cryptographically secure random token with specified length
     *
     * @param length length of the token
     * @return alphanumeric random token
     */
    public static String generateToken(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHANUMERIC.charAt(SECURE_RANDOM.nextInt(ALPHANUMERIC.length())));
        }
        return sb.toString();
    }

    /**
     * Generate a random string for API keys (alphanumeric only, no special characters)
     *
     * @return 64-character alphanumeric random string
     */
    public static String generateApiKeyToken() {
        return generateToken(64);
    }
}