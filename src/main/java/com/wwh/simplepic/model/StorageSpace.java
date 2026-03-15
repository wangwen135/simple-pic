package com.wwh.simplepic.model;

import lombok.Data;

import java.io.File;

/**
 * Storage space model
 * 存储空间模型
 */
@Data
public class StorageSpace {

    private String name;
    private String path;
    private String maxSizeStr; // String representation like "10GB"
    private long maxSize; // in bytes
    private String urlPrefix; // URL prefix for image access (e.g., "http://localhost:8080/image/notes/")
    private boolean allowAnonymous;

    public StorageSpace() {
        this.maxSize = 10L * 1024 * 1024 * 1024; // Default 10GB
        this.maxSizeStr = "10GB";
        this.allowAnonymous = false;
    }

    public String getMaxSizeStr() {
        return maxSizeStr;
    }

    public void setMaxSizeStr(String maxSizeStr) {
        this.maxSizeStr = maxSizeStr;
        this.maxSize = parseSize(maxSizeStr);
    }

    public long getMaxSizeInBytes() {
        return maxSize;
    }

    public void setMaxSize(String sizeStr) {
        this.maxSizeStr = sizeStr;
        this.maxSize = parseSize(sizeStr);
    }

    private long parseSize(String sizeStr) {
        if (sizeStr == null || sizeStr.isEmpty()) {
            return 10L * 1024 * 1024 * 1024; // Default 10GB
        }
        sizeStr = sizeStr.trim().toUpperCase();
        try {
            if (sizeStr.endsWith("GB")) {
                return Long.parseLong(sizeStr.replace("GB", "")) * 1024 * 1024 * 1024;
            } else if (sizeStr.endsWith("MB")) {
                return Long.parseLong(sizeStr.replace("MB", "")) * 1024 * 1024;
            } else if (sizeStr.endsWith("KB")) {
                return Long.parseLong(sizeStr.replace("KB", "")) * 1024;
            } else {
                return Long.parseLong(sizeStr);
            }
        } catch (NumberFormatException e) {
            return 10L * 1024 * 1024 * 1024; // Default 10GB
        }
    }

    public String getFormattedMaxSize() {
        return maxSizeStr != null ? maxSizeStr : formatBytes(maxSize);
    }

    private String formatBytes(long bytes) {
        if (bytes >= 1024 * 1024 * 1024) {
            return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        } else if (bytes >= 1024 * 1024) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        } else if (bytes >= 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else {
            return bytes + " B";
        }
    }

    public File getStorageDirectory() {
        return new File(path);
    }

    public File getThumbnailsDirectory() {
        return new File(path, ".thumbnails");
    }
}