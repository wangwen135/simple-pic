package com.simplepic.service;

import com.simplepic.util.SimplePicUtils;
import net.coobird.thumbnailator.Thumbnails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Thumbnail service
 * 缩略图服务
 */
@Service
public class ThumbnailService {

    private static final Logger logger = LoggerFactory.getLogger(ThumbnailService.class);

    private static final int THUMBNAIL_WIDTH = 300;
    private static final int THUMBNAIL_HEIGHT = 300;

    @Autowired
    private StorageService storageService;

    /**
     * Generate thumbnail for an image
     */
    public File generateThumbnail(File imageFile, String storageSpace) throws IOException {
        com.simplepic.model.StorageSpace space = storageService.getStorageSpace(storageSpace);
        if (space == null) {
            throw new IOException("Storage space not found: " + storageSpace);
        }

        // Get relative path from storage directory
        String relativePath = getRelativePath(imageFile, space.getStorageDirectory());

        // Create thumbnail directory path
        File thumbnailDir = new File(space.getThumbnailsDirectory(),
                relativePath.substring(0, relativePath.lastIndexOf(File.separator)));

        if (!thumbnailDir.exists()) {
            thumbnailDir.mkdirs();
        }

        // Create thumbnail file
        File thumbnailFile = new File(space.getThumbnailsDirectory(), relativePath);

        // Generate thumbnail
        Thumbnails.of(imageFile)
                .size(THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT)
                .outputQuality(0.8)
                .toFile(thumbnailFile);

        logger.debug("Thumbnail generated: {}", thumbnailFile.getAbsolutePath());
        return thumbnailFile;
    }

    /**
     * Get thumbnail file
     */
    public File getThumbnail(String path, String storageSpace) {
        com.simplepic.model.StorageSpace space = storageService.getStorageSpace(storageSpace);
        if (space == null) {
            return null;
        }

        File thumbnailFile = new File(space.getThumbnailsDirectory(), path.replace("/", File.separator));
        if (thumbnailFile.exists()) {
            return thumbnailFile;
        }

        return null;
    }

    /**
     * Get or create thumbnail
     */
    public File getOrCreateThumbnail(String path, String storageSpace) throws IOException {
        com.simplepic.model.StorageSpace space = storageService.getStorageSpace(storageSpace);
        if (space == null) {
            throw new IOException("Storage space not found: " + storageSpace);
        }

        File thumbnailFile = getThumbnail(path, storageSpace);
        if (thumbnailFile != null) {
            return thumbnailFile;
        }

        // Generate thumbnail from original
        File originalFile = new File(space.getStorageDirectory(), path.replace("/", File.separator));
        if (!originalFile.exists()) {
            throw new IOException("Original image not found: " + path);
        }

        return generateThumbnail(originalFile, storageSpace);
    }

    /**
     * Generate thumbnails for all images in a storage space
     */
    public void generateAllThumbnails(String storageSpace) {
        com.simplepic.model.StorageSpace space = storageService.getStorageSpace(storageSpace);
        if (space == null) {
            logger.warn("Storage space not found: {}", storageSpace);
            return;
        }

        File storageDir = space.getStorageDirectory();
        if (!storageDir.exists()) {
            logger.warn("Storage directory not found: {}", storageDir.getAbsolutePath());
            return;
        }

        List<File> images = findAllImages(storageDir);
        logger.info("Found {} images in storage space {}", images.size(), storageSpace);

        int successCount = 0;
        for (File imageFile : images) {
            try {
                File thumbnailFile = getThumbnail(getRelativePath(imageFile, storageDir).replace(File.separator, "/"), storageSpace);
                if (thumbnailFile == null) {
                    generateThumbnail(imageFile, storageSpace);
                    successCount++;
                }
            } catch (IOException e) {
                logger.error("Failed to generate thumbnail for {}", imageFile.getName(), e);
            }
        }

        logger.info("Generated {} new thumbnails in storage space {}", successCount, storageSpace);
    }

    /**
     * Delete thumbnail
     */
    public boolean deleteThumbnail(String path, String storageSpace) {
        com.simplepic.model.StorageSpace space = storageService.getStorageSpace(storageSpace);
        if (space == null) {
            return false;
        }

        File thumbnailFile = new File(space.getThumbnailsDirectory(), path.replace("/", File.separator));
        if (thumbnailFile.exists()) {
            return thumbnailFile.delete();
        }

        return false;
    }

    /**
     * Find all image files in directory recursively
     */
    private List<File> findAllImages(File dir) {
        List<File> images = new ArrayList<>();
        File[] files = dir.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.isFile() && isImageFile(file.getName())) {
                    images.add(file);
                } else if (file.isDirectory() && !file.getName().equals(".thumbnails")) {
                    images.addAll(findAllImages(file));
                }
            }
        }

        return images;
    }

    /**
     * Get relative path from base directory
     */
    private String getRelativePath(File file, File baseDir) {
        return SimplePicUtils.getRelativePath(file, baseDir);
    }

    /**
     * Check if file is an image
     */
    private boolean isImageFile(String filename) {
        String lower = filename.toLowerCase();
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg") ||
               lower.endsWith(".png") || lower.endsWith(".gif") ||
               lower.endsWith(".webp");
    }

    /**
     * Scheduled task to generate missing thumbnails
     * Runs every day at 3 AM
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void scheduledThumbnailGeneration() {
        logger.info("Starting scheduled thumbnail generation");

        List<com.simplepic.model.StorageSpace> spaces = storageService.getAllStorageSpaces();
        for (com.simplepic.model.StorageSpace space : spaces) {
            try {
                generateAllThumbnails(space.getName());
            } catch (Exception e) {
                logger.error("Error generating thumbnails for storage space {}", space.getName(), e);
            }
        }

        logger.info("Scheduled thumbnail generation completed");
    }
}