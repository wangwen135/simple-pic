package com.simplepic.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Login session model
 * 登录会话模型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginSession {

    private String username;
    private String role;
    private String[] storageSpaces;
    private String currentStorageSpace;
    private long loginTime;
    private long expiryTime;
    private boolean rememberMe;

    public LoginSession(String username, String role, String[] storageSpaces) {
        this(username, role, storageSpaces, true);
    }

    public LoginSession(String username, String role, String[] storageSpaces, boolean rememberMe) {
        this.username = username;
        this.role = role;
        this.storageSpaces = storageSpaces;
        this.currentStorageSpace = storageSpaces.length > 0 ? storageSpaces[0] : null;
        this.loginTime = System.currentTimeMillis();
        this.rememberMe = rememberMe;
        long expiryDuration = rememberMe ? 30L * 24 * 60 * 60 * 1000 : 24 * 60 * 60 * 1000; // 30 days or 1 day
        this.expiryTime = System.currentTimeMillis() + expiryDuration;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > expiryTime;
    }

    public void extendExpiry() {
        long expiryDuration = rememberMe ? 30L * 24 * 60 * 60 * 1000 : 24 * 60 * 60 * 1000;
        this.expiryTime = System.currentTimeMillis() + expiryDuration;
    }

    public void extendExpiry(boolean rememberMe) {
        this.rememberMe = rememberMe;
        long expiryDuration = rememberMe ? 30L * 24 * 60 * 60 * 1000 : 24 * 60 * 60 * 1000;
        this.expiryTime = System.currentTimeMillis() + expiryDuration;
    }
}