package com.wwh.simplepic.util;

import java.util.Arrays;
import java.util.List;

/**
 * Application constants
 * 应用常量
 */
public final class Constants {

    private Constants() {
        // Prevent instantiation
    }

    /**
     * Directory names
     * 目录名称常量
     */
    public static final class Directories {
        public static final String THUMBNAILS = ".thumbnails";
        public static final String WATERMARKS = ".watermarks";

        private Directories() {
            // Prevent instantiation
        }
    }

    /**
     * User roles
     * 用户角色常量
     */
    public static final class Roles {
        public static final String ADMIN = "ADMIN";
        public static final String USER = "USER";

        private Roles() {
            // Prevent instantiation
        }
    }

    /**
     * File extensions
     * 文件扩展名常量
     */
    public static final class Extensions {
        /**
         * Supported image file extensions
         * 支持的图片文件扩展名
         */
        public static final List<String> IMAGES = Arrays.asList(
                "jpg", "jpeg", "png", "gif", "webp", "svg"
        );

        private Extensions() {
            // Prevent instantiation
        }
    }

    /**
     * Configuration keys
     * 配置键常量
     */
    public static final class ConfigKeys {
        public static final String SIMPLE_PIC = "simple-pic";
        public static final String SYSTEM = "system";
        public static final String STORAGE_SPACES = "storage-spaces";
        public static final String USERS = "users";
        public static final String API_KEYS = "api-keys";
        public static final String WATERMARK = "watermark";
        public static final String SECURITY = "security";
        public static final String RATE_LIMIT = "rate-limit";
        public static final String FRONTEND = "frontend";
        public static final String LOGIN_LOCKOUT = "login-lockout";

        // Legacy keys
        public static final String DOMAIN = "domain"; // legacy, use URL_PREFIX instead
        public static final String URL_PREFIX = "url-prefix";

        private ConfigKeys() {
            // Prevent instantiation
        }
    }

    /**
     * Cache configuration
     * 缓存配置常量
     */
    public static final class Cache {
        /**
         * Maximum recursion depth for file operations
         * 文件操作的最大递归深度
         */
        public static final int MAX_RECURSION_DEPTH = 1000;

        /**
         * Storage statistics cache TTL in minutes
         * 存储统计缓存过期时间（分钟）
         */
        public static final int STATS_CACHE_TTL_MINUTES = 5;

        /**
         * Storage statistics cache maximum size
         * 存储统计缓存最大条目数
         */
        public static final int STATS_CACHE_MAX_SIZE = 100;

        private Cache() {
            // Prevent instantiation
        }
    }
}
