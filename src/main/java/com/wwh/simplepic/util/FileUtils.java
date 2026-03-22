package com.wwh.simplepic.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.function.Predicate;

/**
 * File utility class for common file operations
 * 文件工具类 - 用于通用文件操作
 */
public class FileUtils {

    /**
     * Supported image file extensions
     * 支持的图片文件扩展名
     */
    private static final List<String> IMAGE_EXTENSIONS = Constants.Extensions.IMAGES;

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

    /**
     * Check if filename has image extension
     * 检查文件名是否具有图片扩展名
     * This method is a simple check, for more robust validation use other methods
     *
     * @param filename the filename to check
     * @return true if the file has an image extension
     */
    public static boolean hasImageExtension(String filename) {
        return hasExtension(filename, "jpg", "jpeg", "png", "gif", "webp", "svg");
    }

    /**
     * Calculate directory size recursively
     * 递归计算目录大小
     *
     * @param dir the directory to calculate
     * @return size in bytes
     */
    public static long calculateDirectorySize(File dir) {
        return calculateDirectorySize(dir, 0, Constants.Cache.MAX_RECURSION_DEPTH);
    }

    /**
     * Calculate directory size recursively with depth limit
     * 递归计算目录大小（带深度限制）
     *
     * @param dir   the directory to calculate
     * @param depth current recursion depth
     * @param maxDepth maximum recursion depth
     * @return size in bytes
     */
    private static long calculateDirectorySize(File dir, int depth, int maxDepth) {
        if (depth >= maxDepth) {
            return 0;
        }

        long size = 0;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    size += file.length();
                } else if (file.isDirectory() && !file.getName().equals(Constants.Directories.THUMBNAILS)) {
                    size += calculateDirectorySize(file, depth + 1, maxDepth);
                }
            }
        }
        return size;
    }

    /**
     * Count files in directory by predicate
     * 按条件统计目录中的文件数量
     *
     * @param dir            the directory to count
     * @param filePredicate  the predicate to filter files (null to count all files)
     * @param dirPredicate   the predicate to filter directories (null to traverse all directories)
     * @return array of [fileCount, directoryCount]
     */
    public static int[] countFiles(File dir, Predicate<String> filePredicate, Predicate<String> dirPredicate) {
        return countFiles(dir, filePredicate, dirPredicate, 0, Constants.Cache.MAX_RECURSION_DEPTH);
    }

    /**
     * Count files in directory by predicate with depth limit
     * 按条件统计目录中的文件数量（带深度限制）
     *
     * @param dir            the directory to count
     * @param filePredicate  the predicate to filter files
     * @param dirPredicate   the predicate to filter directories
     * @param depth          current recursion depth
     * @param maxDepth       maximum recursion depth
     * @return array of [fileCount, directoryCount]
     */
    private static int[] countFiles(File dir, Predicate<String> filePredicate, Predicate<String> dirPredicate, int depth, int maxDepth) {
        if (depth >= maxDepth) {
            return new int[]{0, 0};
        }

        int fileCount = 0;
        int dirCount = 0;

        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    if (filePredicate == null || filePredicate.test(file.getName())) {
                        fileCount++;
                    }
                } else if (file.isDirectory()) {
                    boolean shouldSkip = dirPredicate != null && dirPredicate.test(file.getName());
                    if (!shouldSkip) {
                        dirCount++;
                        int[] subCounts = countFiles(file, filePredicate, dirPredicate, depth + 1, maxDepth);
                        fileCount += subCounts[0];
                        dirCount += subCounts[1];
                    }
                }
            }
        }

        return new int[]{fileCount, dirCount};
    }

    /**
     * Ensure directory exists, create if not
     * 确保目录存在，不存在则创建
     *
     * @param dir the directory to ensure
     * @return true if directory exists or was created successfully
     */
    public static boolean ensureDirectoryExists(File dir) {
        if (dir == null) {
            return false;
        }
        if (dir.exists()) {
            return dir.isDirectory();
        }
        return dir.mkdirs();
    }

    /**
     * Copy file from source to destination
     * 复制文件
     *
     * @param source the source file
     * @param dest   the destination file
     * @return true if copy was successful
     */
    public static boolean copyFile(File source, File dest) {
        if (source == null || dest == null || !source.exists()) {
            return false;
        }
        try {
            Files.copy(source.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Move file from source to destination
     * 移动文件
     *
     * @param source the source file
     * @param dest   the destination file
     * @return true if move was successful
     */
    public static boolean moveFile(File source, File dest) {
        if (source == null || dest == null || !source.exists()) {
            return false;
        }
        // Ensure parent directory exists
        File parentDir = dest.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }
        return source.renameTo(dest);
    }

    /**
     * Delete file or directory recursively
     * 删除文件或目录（递归）
     *
     * @param file the file or directory to delete
     * @return true if deletion was successful
     */
    public static boolean deleteFile(File file) {
        if (file == null || !file.exists()) {
            return false;
        }
        if (file.isDirectory()) {
            return deleteDirectoryRecursive(file);
        }
        return file.delete();
    }

    /**
     * Delete directory recursively
     * 递归删除目录
     *
     * @param dir the directory to delete
     * @return true if deletion was successful
     */
    private static boolean deleteDirectoryRecursive(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectoryRecursive(file);
                } else {
                    file.delete();
                }
            }
        }
        return dir.delete();
    }

    /**
     * Get path separator as string
     * 获取路径分隔符
     *
     * @return path separator
     */
    public static String getPathSeparator() {
        return File.separator;
    }

    /**
     * Normalize path separators to system format
     * 规范化路径分隔符为系统格式
     *
     * @param path the path to normalize
     * @return normalized path
     */
    public static String normalizePath(String path) {
        if (path == null || path.isEmpty()) {
            return path;
        }
        return path.replace("/", File.separator).replace("\\", File.separator);
    }
}
