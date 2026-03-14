package com.simplepic.util;

import java.io.File;
import java.util.List;

/**
 * File utility class for common file operations
 * 文件工具类 - 用于通用文件操作
 */
public class FileUtils {

    /**
     * Supported image file extensions
     * 支持的图片文件扩展名
     */
    private static final List<String> IMAGE_EXTENSIONS = List.of(
            "jpg", "jpeg", "png", "gif", "webp", "svg"
    );

    /**
     * Check if file is an image based on its extension
     * 根据扩展名检查文件是否为图片
     *
     * @param filename the filename to check
     * @return true if the file is an image, false otherwise
     */
    public static boolean isImageFile(String filename) {
        if (filename == null || filename.isEmpty()) {
            return false;
        }
        String lower = filename.toLowerCase();
        int lastDot = lower.lastIndexOf('.');
        if (lastDot < 0 || lastDot == lower.length() - 1) {
            return false;
        }
        String extension = lower.substring(lastDot + 1);
        return IMAGE_EXTENSIONS.contains(extension);
    }

    /**
     * Get relative path from base directory
     * 获取相对于基础目录的路径
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
     * 获取文件扩展名
     *
     * @param filename the filename
     * @return file extension (without dot), empty string if no extension
     */
    public static String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }
        int lastDot = filename.lastIndexOf('.');
        if (lastDot > 0 && lastDot < filename.length() - 1) {
            return filename.substring(lastDot + 1);
        }
        return "";
    }

    /**
     * Check if filename has one of the specified extensions
     * 检查文件名是否具有指定的扩展名之一
     *
     * @param filename    the filename to check
     * @param extensions the list of extensions to check against
     * @return true if the filename has one of the specified extensions
     */
    public static boolean hasExtension(String filename, String... extensions) {
        String ext = getFileExtension(filename).toLowerCase();
        for (String extension : extensions) {
            if (ext.equals(extension.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get file size in human-readable format
     * 获取人类可读格式的文件大小
     *
     * @param bytes file size in bytes
     * @return formatted string (e.g., "1.5 MB")
     */
    public static String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }

    /**
     * Parse file size string to bytes
     * 解析文件大小字符串为字节数
     *
     * @param sizeStr size string (e.g., "10MB", "1GB")
     * @return size in bytes, or -1 if invalid format
     */
    public static long parseFileSize(String sizeStr) {
        if (sizeStr == null || sizeStr.isEmpty()) {
            return -1;
        }
        sizeStr = sizeStr.trim().toUpperCase();
        try {
            if (sizeStr.endsWith("GB")) {
                return Long.parseLong(sizeStr.substring(0, sizeStr.length() - 2)) * 1024 * 1024 * 1024;
            } else if (sizeStr.endsWith("MB")) {
                return Long.parseLong(sizeStr.substring(0, sizeStr.length() - 2)) * 1024 * 1024;
            } else if (sizeStr.endsWith("KB")) {
                return Long.parseLong(sizeStr.substring(0, sizeStr.length() - 2)) * 1024;
            } else if (sizeStr.endsWith("B")) {
                return Long.parseLong(sizeStr.substring(0, sizeStr.length() - 1));
            } else {
                // Default to MB if no unit specified
                return Long.parseLong(sizeStr) * 1024 * 1024;
            }
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
