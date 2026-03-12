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

    public LoginSession(String username, String role, String[] storageSpaces) {
        this.username = username;
        this.role = role;
        this.storageSpaces = storageSpaces;
        this.currentStorageSpace = storageSpaces.length > 0 ? storageSpaces[0] : null;
        this.loginTime = System.currentTimeMillis();
        this.expiryTime = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000); // 30 days
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > expiryTime;
    }

    public void extendExpiry() {
        this.expiryTime = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000); // Extend to 30 days
    }
}