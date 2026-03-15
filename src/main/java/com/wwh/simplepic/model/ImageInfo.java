package com.wwh.simplepic.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Image information model
 * 图片信息模型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImageInfo {

    private String path;
    private String name;
    private long size;
    private long lastModified;
    private String storageSpace;
    private boolean hasThumbnail;

    public ImageInfo(File file, String storageSpace, String relativePath) {
        this.path = relativePath;
        this.name = file.getName();
        this.size = file.length();
        this.lastModified = file.lastModified();
        this.storageSpace = storageSpace;

        // Check if thumbnail exists
        File thumbnailDir = new File(file.getParentFile().getParentFile(), ".thumbnails");
        File thumbnailFile = new File(thumbnailDir, relativePath.replace("/", File.separator));
        this.hasThumbnail = thumbnailFile.exists();
    }

    public String getFormattedSize() {
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

    public String getFormattedDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(new Date(lastModified));
    }
}