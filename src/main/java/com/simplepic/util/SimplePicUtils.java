package com.simplepic.util;

import javax.servlet.http.HttpServletRequest;
import java.io.File;

/**
 * Utility class for common functions
 * 工具类
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
     *
     * @param path the path to validate
     * @return true if path is safe, false otherwise
     */
    public static boolean isPathSafe(String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }
        // Check for path traversal
        if (path.contains("..") || path.contains("\\\\")) {
            return false;
        }
        // Check for absolute paths
        File file = new File(path);
        return !file.isAbsolute();
    }

    /**
     * Normalize path to prevent path traversal
     *
     * @param path the path to normalize
     * @return normalized safe path
     */
    public static String normalizePath(String path) {
        if (path == null) {
            return "";
        }
        // Replace backslashes with forward slashes
        path = path.replace("\\", "/");
        // Remove path traversal attempts
        path = path.replaceAll("\\.+/", "");
        // Remove leading/trailing slashes and dots
        path = path.replaceAll("^/+|/+$", "");
        path = path.replaceAll("^\\.+|\\.+$", "");
        return path;
    }
}