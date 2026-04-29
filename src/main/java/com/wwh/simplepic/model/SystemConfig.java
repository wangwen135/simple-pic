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
    private boolean watermarkEnabled;

    // 上传限制
    private int maxFileSizeMB;
    private String allowedFileTypes; // 逗号分隔的扩展名

    private List<StorageSpace> storageSpaces = new ArrayList<>();
    private List<User> users = new ArrayList<>();
    private List<ApiKey> apiKeys = new ArrayList<>();

    private boolean rateLimitEnabled;
    private int maxRequests;
    private int timeWindow;

    // 登录安全设置
    private boolean loginLockoutEnabled;
    private int maxFailedAttempts;
    private int lockoutMinutes;

    // 防盗链设置
    private boolean hotlinkProtectionEnabled;
    private String allowedReferers; // 白名单域名（逗号分隔）
    private String hotlinkResponse; // "generated" / "image" / "403"
    private String hotlinkImagePath; // 自定义提示图路径

    @Data
    public static class StorageSpace {
        private String name;
        private String path;
        private String maxSize;
        private String urlPrefix; // URL prefix for image access
        private boolean allowAnonymous;
        private WatermarkConfig watermark; // 每个存储空间独立的水印配置
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
        private String remark; // 备注信息
    }
}