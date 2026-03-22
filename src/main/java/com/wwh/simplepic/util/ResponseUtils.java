package com.wwh.simplepic.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Unified API response builder utility
 * 统一API响应构建工具类
 * <p>
 * Provides consistent response format across all controllers
 * 为所有控制器提供统一的响应格式
 */
public class ResponseUtils {

    /**
     * Create a success response
     * 创建成功响应
     */
    public static Map<String, Object> success() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        return response;
    }

    /**
     * Create a success response with data
     * 创建带数据的成功响应
     */
    public static Map<String, Object> success(String key, Object value) {
        Map<String, Object> response = success();
        response.put(key, value);
        return response;
    }

    /**
     * Create a success response with multiple data fields
     * 创建带多个数据的成功响应
     */
    public static Map<String, Object> success(Map<String, Object> data) {
        Map<String, Object> response = success();
        response.putAll(data);
        return response;
    }

    /**
     * Create an error response with i18n support
     * 创建带国际化支持的错误响应
     */
    public static Map<String, Object> error(String errorKey) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", ErrorMessages.getZh(errorKey));
        response.put("error_en", ErrorMessages.getEn(errorKey));
        return response;
    }

    /**
     * Create an error response with custom message (same for both languages)
     * 创建带自定义消息的错误响应（中英文相同）
     */
    public static Map<String, Object> error(String messageZh, String messageEn) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", messageZh);
        response.put("error_en", messageEn);
        return response;
    }

    /**
     * Create an error response with single message (used for both languages)
     * 创建带单一消息的错误响应（用于中英文相同）
     */
    public static Map<String, Object> errorMessage(String message) {
        return error(message, message);
    }

    /**
     * Create an error response with error key and additional detail
     * 创建带错误键和附加详情的错误响应
     */
    public static Map<String, Object> errorWithDetail(String errorKey, String detail) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", ErrorMessages.getZh(errorKey) + ": " + detail);
        response.put("error_en", ErrorMessages.getEn(errorKey) + ": " + detail);
        return response;
    }

    /**
     * Create an unauthorized response (401)
     * 创建未授权响应 (401)
     */
    public static Map<String, Object> unauthorized() {
        return error("unauthorized");
    }

    /**
     * Create a forbidden response (403)
     * 创建禁止访问响应 (403)
     */
    public static Map<String, Object> forbidden() {
        return error("forbidden");
    }

    /**
     * Create a validation error response
     * 创建验证错误响应
     */
    public static Map<String, Object> validationError(String field, String messageZh, String messageEn) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", field + " " + messageZh);
        response.put("error_en", field + " " + messageEn);
        return response;
    }
}
