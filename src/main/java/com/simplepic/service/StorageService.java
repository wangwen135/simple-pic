package com.simplepic.service;

import com.simplepic.model.StorageSpace;
import com.simplepic.model.StorageStats;
import com.simplepic.model.SystemConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Storage service
 * 存储服务
 */
@Service
public class StorageService {

    private static final Logger logger = LoggerFactory.getLogger(StorageService.class);

    @Autowired
    private ConfigService configService;

    @Autowired
    private UserService userService;

    private final Map<String, StorageStats> statsCache = new ConcurrentHashMap<>();
    private final Map<String, Long> lastStatsUpdate = new ConcurrentHashMap<>();

    /**
     * Get all storage spaces
     */
    public List<StorageSpace> getAllStorageSpaces() {
        SystemConfig config = configService.getConfig();
        if (config == null || config.getStorageSpaces() == null) {
            return new ArrayList<>();
        }

        return config.getStorageSpaces().stream()
                .map(spaceConfig -> {
                    StorageSpace space = new StorageSpace();
                    space.setName(spaceConfig.getName());
                    space.setPath(spaceConfig.getPath());
                    space.setMaxSize(spaceConfig.getMaxSize());
                    space.setDomain(spaceConfig.getDomain());
                    space.setAllowAnonymous(spaceConfig.isAllowAnonymous());
                    return space;
                })
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Get storage space by name
     */
    public StorageSpace getStorageSpace(String name) {
        SystemConfig config = configService.getConfig();
        if (config == null || config.getStorageSpaces() == null) {
            return null;
        }

        for (SystemConfig.StorageSpace spaceConfig : config.getStorageSpaces()) {
            if (spaceConfig.getName().equals(name)) {
                StorageSpace space = new StorageSpace();
                space.setName(spaceConfig.getName());
                space.setPath(spaceConfig.getPath());
                space.setMaxSize(spaceConfig.getMaxSize());
                space.setDomain(spaceConfig.getDomain());
                space.setAllowAnonymous(spaceConfig.isAllowAnonymous());
                return space;
            }
        }

        return null;
    }

    /**
     * Create storage space
     */
    public boolean createStorageSpace(String name, String path, String maxSize, String domain) {
        return createStorageSpace(name, path, maxSize, domain, false);
    }

    /**
     * Validate storage space name
     */
    private boolean isValidStorageSpaceName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        // Only allow letters, numbers, underscore, hyphen, and Chinese characters
        return name.matches("^[\\u4e00-\\u9fa5a-zA-Z0-9_-]+$");
    }

    /**
     * Validate domain format
     */
    private boolean isValidDomain(String domain) {
        if (domain == null || domain.trim().isEmpty()) {
            return false;
        }
        // Must start with http:// or https://
        if (!domain.matches("^https?://.*")) {
            return false;
        }
        try {
            new java.net.URL(domain);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Create storage space
     */
    public boolean createStorageSpace(String name, String path, String maxSize, String domain, boolean allowAnonymous) {
        SystemConfig config = configService.getConfig();
        if (config == null) {
            return false;
        }

        // Validate name
        if (!isValidStorageSpaceName(name)) {
            logger.warn("Invalid storage space name: {}", name);
            return false;
        }

        // Check if storage space already exists
        if (getStorageSpace(name) != null) {
            logger.warn("Storage space {} already exists", name);
            return false;
        }

        // Validate domain
        if (!isValidDomain(domain)) {
            logger.warn("Invalid domain: {}", domain);
            return false;
        }

        // Validate path and create directory if not exists
        if (path == null || path.trim().isEmpty()) {
            logger.warn("Path is empty");
            return false;
        }

        File storageDir = new File(path);
        if (!storageDir.exists()) {
            try {
                storageDir.mkdirs();
                logger.info("Created storage directory: {}", path);
            } catch (Exception e) {
                logger.error("Failed to create storage directory: {}", path, e);
                return false;
            }
        }

        SystemConfig.StorageSpace spaceConfig = new SystemConfig.StorageSpace();
        spaceConfig.setName(name);
        spaceConfig.setPath(path);
        spaceConfig.setMaxSize(maxSize);
        spaceConfig.setDomain(domain);
        spaceConfig.setAllowAnonymous(allowAnonymous);

        if (config.getStorageSpaces() == null) {
            config.setStorageSpaces(new ArrayList<>());
        }
        config.getStorageSpaces().add(spaceConfig);

        // Automatically assign new storage space to all admin users
        if (config.getUsers() != null) {
            for (SystemConfig.User user : config.getUsers()) {
                if ("ADMIN".equals(user.getRole()) && !user.getStorageSpaces().contains(name)) {
                    user.getStorageSpaces().add(name);
                    logger.info("Automatically assigned storage space {} to admin user {}", name, user.getUsername());
                }
            }
        }

        configService.saveConfig(config);

        // Create thumbnails directory if not exists
        File thumbnailsDir = new File(path, ".thumbnails");
        if (!thumbnailsDir.exists()) {
            try {
                thumbnailsDir.mkdirs();
                logger.info("Created thumbnails directory: {}", thumbnailsDir.getAbsolutePath());
            } catch (Exception e) {
                logger.error("Failed to create thumbnails directory: {}", thumbnailsDir.getAbsolutePath(), e);
                // Continue anyway, this is not critical
            }
        }

        logger.info("Storage space {} created", name);
        return true;
    }

    /**
     * Update storage space
     */
    public boolean updateStorageSpace(String name, String path, String maxSize, String domain) {
        return updateStorageSpace(name, path, maxSize, domain, false);
    }

    /**
     * Update storage space
     */
    public boolean updateStorageSpace(String name, String path, String maxSize, String domain, boolean allowAnonymous) {
        SystemConfig config = configService.getConfig();
        if (config == null || config.getStorageSpaces() == null) {
            return false;
        }

        // Validate domain
        if (!isValidDomain(domain)) {
            logger.warn("Invalid domain: {}", domain);
            return false;
        }

        // Validate path and create directory if not exists
        if (path == null || path.trim().isEmpty()) {
            logger.warn("Path is empty");
            return false;
        }

        File storageDir = new File(path);
        if (!storageDir.exists()) {
            try {
                storageDir.mkdirs();
                logger.info("Created storage directory: {}", path);
            } catch (Exception e) {
                logger.error("Failed to create storage directory: {}", path, e);
                return false;
            }
        }

        // Create thumbnails directory if not exists
        File thumbnailsDir = new File(path, ".thumbnails");
        if (!thumbnailsDir.exists()) {
            try {
                thumbnailsDir.mkdirs();
                logger.info("Created thumbnails directory: {}", thumbnailsDir.getAbsolutePath());
            } catch (Exception e) {
                logger.error("Failed to create thumbnails directory: {}", thumbnailsDir.getAbsolutePath(), e);
                // Continue anyway, this is not critical
            }
        }

        for (SystemConfig.StorageSpace spaceConfig : config.getStorageSpaces()) {
            if (spaceConfig.getName().equals(name)) {
                spaceConfig.setPath(path);
                spaceConfig.setMaxSize(maxSize);
                spaceConfig.setDomain(domain);
                spaceConfig.setAllowAnonymous(allowAnonymous);

                configService.saveConfig(config);
                logger.info("Storage space {} updated", name);
                return true;
            }
        }

        return false;
    }

    /**
     * Delete storage space
     */
    public boolean deleteStorageSpace(String name) {
        SystemConfig config = configService.getConfig();
        if (config == null || config.getStorageSpaces() == null) {
            return false;
        }

        for (SystemConfig.StorageSpace spaceConfig : config.getStorageSpaces()) {
            if (spaceConfig.getName().equals(name)) {
                config.getStorageSpaces().remove(spaceConfig);
                configService.saveConfig(config);
                logger.info("Storage space {} deleted", name);

                // Clear cache
                statsCache.remove(name);
                lastStatsUpdate.remove(name);

                return true;
            }
        }

        return false;
    }

    /**
     * Get storage statistics
     */
    public StorageStats getStorageStats(String name) {
        // Check cache (5 minute TTL)
        Long lastUpdate = lastStatsUpdate.get(name);
        if (lastUpdate != null && System.currentTimeMillis() - lastUpdate < 300000) {
            return statsCache.get(name);
        }

        StorageSpace space = getStorageSpace(name);
        if (space == null) {
            return null;
        }

        File storageDir = space.getStorageDirectory();
        if (!storageDir.exists()) {
            return null;
        }

        StorageStats stats = new StorageStats();
        stats.setStorageSpaceName(name);
        stats.setTotalSize(space.getMaxSizeInBytes());

        // Calculate usage
        long usedSize = calculateDirectorySize(storageDir);
        stats.setUsedSize(usedSize);
        stats.setFreeSize(space.getMaxSizeInBytes() - usedSize);

        // Count images and directories
        int[] counts = countImagesAndDirectories(storageDir);
        stats.setImageCount(counts[0]);
        stats.setDirectoryCount(counts[1]);

        stats.setUsagePercentage((double) usedSize / space.getMaxSizeInBytes() * 100);

        // Update cache
        statsCache.put(name, stats);
        lastStatsUpdate.put(name, System.currentTimeMillis());

        return stats;
    }

    /**
     * Calculate directory size recursively
     */
    private long calculateDirectorySize(File dir) {
        long size = 0;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    size += file.length();
                } else if (file.isDirectory() && !file.getName().equals(".thumbnails")) {
                    size += calculateDirectorySize(file);
                }
            }
        }
        return size;
    }

    /**
     * Count images and directories
     */
    private int[] countImagesAndDirectories(File dir) {
        int imageCount = 0;
        int dirCount = 0;

        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && isImageFile(file.getName())) {
                    imageCount++;
                } else if (file.isDirectory() && !file.getName().equals(".thumbnails")) {
                    dirCount++;
                    int[] subCounts = countImagesAndDirectories(file);
                    imageCount += subCounts[0];
                    dirCount += subCounts[1];
                }
            }
        }

        return new int[]{imageCount, dirCount};
    }

    /**
     * Check if file is an image
     */
    private boolean isImageFile(String filename) {
        String lower = filename.toLowerCase();
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg") ||
               lower.endsWith(".png") || lower.endsWith(".gif") ||
               lower.endsWith(".webp") || lower.endsWith(".svg");
    }

    /**
     * Check if storage space has enough space
     */
    public boolean hasEnoughSpace(String name, long fileSize) {
        StorageStats stats = getStorageStats(name);
        return stats != null && stats.getFreeSize() >= fileSize;
    }

    /**
     * Clear stats cache
     */
    public void clearStatsCache(String name) {
        if (name != null) {
            statsCache.remove(name);
            lastStatsUpdate.remove(name);
        } else {
            statsCache.clear();
            lastStatsUpdate.clear();
        }
    }

    /**
     * Get all storage stats
     */
    public List<StorageStats> getAllStorageStats() {
        List<StorageStats> allStats = new ArrayList<>();
        for (StorageSpace space : getAllStorageSpaces()) {
            StorageStats stats = getStorageStats(space.getName());
            if (stats != null) {
                allStats.add(stats);
            }
        }
        return allStats;
    }
}