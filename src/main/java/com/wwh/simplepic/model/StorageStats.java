package com.wwh.simplepic.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Storage statistics model
 * 存储统计模型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StorageStats {

    private String storageSpaceName;
    private long totalSize;
    private long usedSize;
    private long freeSize;
    private int imageCount;
    private int directoryCount;
    private double usagePercentage;

    public String getFormattedTotalSize() {
        return formatSize(totalSize);
    }

    public String getFormattedUsedSize() {
        return formatSize(usedSize);
    }

    public String getFormattedFreeSize() {
        return formatSize(freeSize);
    }

    private String formatSize(long size) {
        if (size >= 1024 * 1024 * 1024) {
            return String.format("%.2f GB", size / (1024.0 * 1024.0 * 1024.0));
        } else if (size >= 1024 * 1024) {
            return String.format("%.2f MB", size / (1024.0 * 1024.0));
        } else if (size >= 1024) {
            return String.format("%.2f KB", size / 1024.0);
        } else {
            return size + " B";
        }
    }
}