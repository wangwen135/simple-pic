package com.wwh.simplepic.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * System configuration model
 * 系统配置模型
 */
@Data
public class SystemConfig {

    private String name;
    private String description;

    private boolean anonymousUploadEnabled;
    private String allowedOrigins; // CORS allowed origins (comma-separated)

    private String theme;
    private int itemsPerPage;

    private List<StorageSpace> storageSpaces = new ArrayList<>();
    private List<User> users = new ArrayList<>();
    private List<ApiKey> apiKeys = new ArrayList<>();

    private boolean watermarkEnabled;
    private String watermarkType;
    private String watermarkContent;
    private String watermarkPosition;
    private double watermarkOpacity;

    private boolean rateLimitEnabled;
    private int maxRequests;
    private int timeWindow;

    // 登录安全设置
    private boolean loginLockoutEnabled;
    private int maxFailedAttempts;
    private int lockoutMinutes;

    @Data
    public static class StorageSpace {
        private String name;
        private String path;
        private String maxSize;
        private String urlPrefix; // URL prefix for image access
        private boolean allowAnonymous;
    }

    @Data
    public static class User {
        private String username;
        private String password;
        private String role;
        private List<String> storageSpaces = new ArrayList<>();
    }

    @Data
    public static class ApiKey {
        private String token;
        private String storageSpace;
    }
}