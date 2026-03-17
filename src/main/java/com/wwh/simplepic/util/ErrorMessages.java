package com.wwh.simplepic.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Error messages with i18n support
 * 国际化错误消息
 */
public class ErrorMessages {

    private static final Map<String, String[]> MESSAGES = new HashMap<>();

    static {
        // Auth errors
        MESSAGES.put("invalid_credentials", new String[]{"用户名或密码错误", "Invalid username or password"});
        MESSAGES.put("ip_locked", new String[]{"IP地址已被锁定", "IP address locked"});
        MESSAGES.put("not_authenticated", new String[]{"未登录", "Not authenticated"});

        // API errors
        MESSAGES.put("api_token_required", new String[]{"需要API密钥", "API token required"});
        MESSAGES.put("invalid_api_token", new String[]{"无效的API密钥", "Invalid API token"});

        // Image errors
        MESSAGES.put("anonymous_upload_not_enabled", new String[]{"匿名上传未启用", "Anonymous upload is not enabled"});
        MESSAGES.put("storage_no_anonymous", new String[]{"该存储空间不允许匿名上传", "This storage space does not allow anonymous upload"});
        MESSAGES.put("upload_failed", new String[]{"上传失败", "Upload failed"});
        MESSAGES.put("failed_to_delete_image", new String[]{"删除图片失败", "Failed to delete image"});
        MESSAGES.put("file_is_empty", new String[]{"文件为空", "File is empty"});
        MESSAGES.put("invalid_filename", new String[]{"无效的文件名", "Invalid filename"});
        MESSAGES.put("file_type_not_allowed", new String[]{"不支持的文件类型", "File type not allowed"});
        MESSAGES.put("storage_quota_exceeded", new String[]{"存储空间配额已用完", "Storage space quota exceeded"});
        MESSAGES.put("storage_space_not_found", new String[]{"存储空间不存在", "Storage space not found"});
        MESSAGES.put("file_not_found", new String[]{"文件不存在", "File not found"});

        // Storage errors
        MESSAGES.put("failed_to_create_storage", new String[]{"创建存储空间失败", "Failed to create storage space"});
        MESSAGES.put("failed_to_update_storage", new String[]{"更新存储空间失败", "Failed to update storage space"});
        MESSAGES.put("failed_to_delete_storage", new String[]{"删除存储空间失败", "Failed to delete storage space"});
        MESSAGES.put("invalid_api_key_index", new String[]{"无效的API密钥索引", "Invalid API key index"});
        MESSAGES.put("failed_to_create_directory", new String[]{"创建目录失败", "Failed to create directory"});
        MESSAGES.put("failed_to_delete_directory", new String[]{"删除目录失败", "Failed to delete directory"});
        MESSAGES.put("failed_to_rename_directory", new String[]{"重命名目录失败", "Failed to rename directory"});
        MESSAGES.put("failed_to_switch_storage", new String[]{"切换存储空间失败", "Failed to switch storage space"});

        // User errors
        MESSAGES.put("failed_to_create_user", new String[]{"创建用户失败", "Failed to create user"});
        MESSAGES.put("failed_to_update_user", new String[]{"更新用户失败", "Failed to update user"});
        MESSAGES.put("failed_to_delete_user", new String[]{"删除用户失败", "Failed to delete user"});
        MESSAGES.put("invalid_api_key", new String[]{"无效的API密钥", "Invalid API key"});

        // System errors
        MESSAGES.put("failed_to_update_config", new String[]{"更新配置失败", "Failed to update config"});
        MESSAGES.put("upload_exceeded", new String[]{"上传失败：超过文件大小限制", "Upload failed: File size exceeded"});
        MESSAGES.put("invalid_file_format", new String[]{"上传失败：不支持的文件格式", "Upload failed: Invalid file format"});

        // Password change errors
        MESSAGES.put("password_changed", new String[]{"密码修改成功", "Password changed successfully"});
        MESSAGES.put("password_change_failed", new String[]{"密码修改失败", "Failed to change password"});
        MESSAGES.put("please_fill_all_fields", new String[]{"请填写所有字段", "Please fill in all fields"});
        MESSAGES.put("passwords_not_match", new String[]{"两次输入的密码不一致", "Passwords do not match"});
        MESSAGES.put("password_too_short", new String[]{"密码长度至少6位", "Password must be at least 6 characters"});
        MESSAGES.put("invalid_current_password", new String[]{"当前密码错误", "Invalid current password"});

        // Rate limit errors
        MESSAGES.put("rate_limit_exceeded", new String[]{"请求过于频繁，请稍后再试", "Rate limit exceeded"});

        // Common errors
        MESSAGES.put("unauthorized", new String[]{"未授权", "Unauthorized"});
        MESSAGES.put("invalid_or_expired_token", new String[]{"令牌无效或已过期", "Invalid or expired token"});
        MESSAGES.put("forbidden", new String[]{"禁止访问", "Forbidden"});
        MESSAGES.put("admin_access_required", new String[]{"需要管理员权限", "Admin access required"});
    }

    /**
     * Get error message
     * @param key message key
     * @return Chinese message (for compatibility)
     */
    public static String get(String key) {
        return getZh(key);
    }

    /**
     * Get Chinese error message
     */
    public static String getZh(String key) {
        String[] messages = MESSAGES.get(key);
        if (messages != null && messages.length > 0) {
            return messages[0];
        }
        return key;
    }

    /**
     * Get English error message
     */
    public static String getEn(String key) {
        String[] messages = MESSAGES.get(key);
        if (messages != null && messages.length > 1) {
            return messages[1];
        }
        return key;
    }

    /**
     * Get both Chinese and English messages
     */
    public static String[] getBoth(String key) {
        return MESSAGES.getOrDefault(key, new String[]{key, key});
    }
}