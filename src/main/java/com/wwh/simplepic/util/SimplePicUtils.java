package com.wwh.simplepic.util;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Utility class for common functions
 * 通用工具类
 */
public class SimplePicUtils {

    /**
     * Extract client IP address from request
     * Priority: X-Forwarded-For -> X-Real-IP -> RemoteAddr
     *
     * @param request HTTP request
     * @return client IP address
     */
    public static String getClientIpAddress(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("X-Real-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        }
        // Handle multiple IPs in X-Forwarded-For (take first one)
        if (ipAddress != null && ipAddress.contains(",")) {
            ipAddress = ipAddress.split(",")[0].trim();
        }
        return ipAddress;
    }

    /**
     * Get relative path from base directory
     *
     * @param file    the file
     * @param baseDir the base directory
     * @return relative path
     */
    public static String getRelativePath(File file, File baseDir) {
        String basePath = baseDir.getAbsolutePath();
        String filePath = file.getAbsolutePath();

        if (filePath.startsWith(basePath)) {
            return filePath.substring(basePath.length() + 1);
        }

        return file.getName();
    }

    /**
     * Get file extension from filename
     *
     * @param filename the filename
     * @return file extension (without dot), empty string if no extension
     */
    public static String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot > 0 && lastDot < filename.length() - 1) {
            return filename.substring(lastDot + 1);
        }
        return "";
    }

    /**
     * Validate path to prevent path traversal attacks
     * 验证路径以防止路径遍历攻击
     *
     * @param path the path to validate (URL encoded or plain)
     * @return true if path is safe, false otherwise
     */
    public static boolean isPathSafe(String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }
        // Decode URL encoding if present
        String decodedPath;
        try {
            decodedPath = URLDecoder.decode(path, "UTF-8");
        } catch (UnsupportedEncodingException | IllegalArgumentException e) {
            return false;
        }
        // Check for obvious path traversal attempts before normalization
        if (decodedPath.contains("..") || decodedPath.contains("\\\\")) {
            return false;
        }
        // Normalize the path
        Path normalized;
        try {
            normalized = Paths.get(decodedPath).normalize();
        } catch (Exception e) {
            return false;
        }
        // Check if normalized path contains ".." (path traversal)
        String normalizedStr = normalized.toString();
        if (normalizedStr.contains("..")) {
            return false;
        }
        // Check for absolute paths
        return !normalized.isAbsolute();
    }

    /**
     * Normalize path to prevent path traversal
     * 规范化路径以防止路径遍历
     *
     * @param path the path to normalize
     * @return normalized safe path
     */
    public static String normalizePath(String path) {
        if (path == null) {
            return "";
        }
        // Decode URL encoding if present
        String decodedPath;
        try {
            decodedPath = URLDecoder.decode(path, "UTF-8");
        } catch (UnsupportedEncodingException | IllegalArgumentException e) {
            decodedPath = path;
        }
        // Normalize using java.nio.file.Path
        Path normalized;
        try {
            normalized = Paths.get(decodedPath).normalize();
        } catch (Exception e) {
            // Fallback to simple normalization
            normalized = Paths.get(decodedPath.replace("\\", "/"));
        }
        String result = normalized.toString();
        // Convert backslashes to forward slashes
        result = result.replace("\\", "/");
        // Remove leading slashes
        result = result.replaceAll("^/+", "");
        return result;
    }

    /**
     * Validate path is within base directory (for extra security)
     * 验证路径在基础目录内（用于额外的安全性）
     *
     * @param path    the path to validate
     * @param baseDir the base directory
     * @return true if path is safe and within base directory
     */
    public static boolean isPathWithinBase(String path, File baseDir) {
        if (!isPathSafe(path)) {
            return false;
        }
        try {
            Path basePath = baseDir.toPath().toAbsolutePath().normalize();
            Path resolvedPath = basePath.resolve(path).normalize();
            return resolvedPath.startsWith(basePath);
        } catch (Exception e) {
            return false;
        }
    }
}