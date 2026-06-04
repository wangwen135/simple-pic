package com.wwh.simplepic.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.function.Predicate;

/**
 * 文件工具类 - 用于通用文件操作
 */
public class FileUtils {

    /**
     * 支持的图片文件扩展名
     */
    private static final List<String> IMAGE_EXTENSIONS = Constants.Extensions.IMAGES;

    /**
     * 根据扩展名检查文件是否为图片
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
     * 获取相对于基础目录的路径
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
     * 获取文件扩展名（不含点号）
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
     * 检查文件名是否具有指定的扩展名之一
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
     * 获取人类可读格式的文件大小（如 "1.5 MB"）
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
     * 解析文件大小字符串为字节数（如 "10MB"、"1GB"），无效格式返回 -1
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
     * 检查文件名是否具有图片扩展名
     */
    public static boolean hasImageExtension(String filename) {
        return hasExtension(filename, "jpg", "jpeg", "png", "gif", "webp", "svg");
    }

    /**
     * 递归计算目录大小
     */
    public static long calculateDirectorySize(File dir) {
        return calculateDirectorySize(dir, 0, Constants.Cache.MAX_RECURSION_DEPTH);
    }

    /**
     * 递归计算目录大小（带深度限制）
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
                } else if (file.isDirectory() && !file.getName().equals(Constants.Directories.THUMBNAILS)
                        && !file.getName().equals(Constants.Directories.WATERMARKS)) {
                    size += calculateDirectorySize(file, depth + 1, maxDepth);
                }
            }
        }
        return size;
    }

    /**
     * 按条件统计目录中的文件和目录数量，返回 [fileCount, directoryCount]
     */
    public static int[] countFiles(File dir, Predicate<String> filePredicate, Predicate<String> dirPredicate) {
        return countFiles(dir, filePredicate, dirPredicate, 0, Constants.Cache.MAX_RECURSION_DEPTH);
    }

    /**
     * 按条件统计目录中的文件数量（带深度限制）
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
     * 确保目录存在，不存在则创建
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
     * 复制文件
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
     * 移动文件
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
     * 删除文件或目录（递归）
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
     * 递归删除目录
     */
    public static boolean deleteDirectoryRecursive(File dir) {
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
     * 获取路径分隔符
     */
    public static String getPathSeparator() {
        return File.separator;
    }

    /**
     * 规范化路径分隔符为系统格式
     */
    public static String normalizePath(String path) {
        if (path == null || path.isEmpty()) {
            return path;
        }
        return path.replace("/", File.separator).replace("\\", File.separator);
    }

    /**
     * 验证路径以防止目录遍历攻击
     */
    public static File validatePath(String userPath, File baseDir) {
        if (userPath == null || userPath.isEmpty()) {
            return null;
        }
        if (baseDir == null || !baseDir.exists()) {
            return null;
        }

        // Check for path traversal patterns
        if (userPath.contains("..") || userPath.contains("~")) {
            return null;
        }

        // Normalize the user path
        String normalizedPath = normalizePath(userPath);

        // Resolve against base directory
        File resolvedFile = new File(baseDir, normalizedPath);

        try {
            // Get canonical paths for comparison
            String baseCanonical = baseDir.getCanonicalPath();
            String resolvedCanonical = resolvedFile.getCanonicalPath();

            // Ensure resolved path is inside base directory, not just sharing its prefix.
            if (!resolvedCanonical.equals(baseCanonical)
                    && !resolvedCanonical.startsWith(baseCanonical + File.separator)) {
                return null;
            }

            return resolvedFile;
        } catch (IOException e) {
            return null;
        }
    }
}
