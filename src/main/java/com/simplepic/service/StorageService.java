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
     * Create storage space
     */
    public boolean createStorageSpace(String name, String path, String maxSize, String domain, boolean allowAnonymous) {
        SystemConfig config = configService.getConfig();
        if (config == null) {
            return false;
        }

        // Check if storage space already exists
        if (getStorageSpace(name) != null) {
            logger.warn("Storage space {} already exists", name);
            return false;
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

        configService.saveConfig(config);

        // Create directories
        File storageDir = new File(path);
        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }

        File thumbnailsDir = new File(path, ".thumbnails");
        if (!thumbnailsDir.exists()) {
            thumbnailsDir.mkdirs();
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