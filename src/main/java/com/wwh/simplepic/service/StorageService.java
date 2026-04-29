package com.wwh.simplepic.service;

import com.wwh.simplepic.model.StorageSpace;
import com.wwh.simplepic.model.StorageStats;
import com.wwh.simplepic.model.SystemConfig;
import com.wwh.simplepic.model.WatermarkConfig;
import com.wwh.simplepic.util.Constants;
import com.wwh.simplepic.util.FileUtils;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 存储服务
 */
@Service
public class StorageService {

    private static final Logger logger = LoggerFactory.getLogger(StorageService.class);

    @Autowired
    private ConfigService configService;

    @Autowired
    private UserService userService;

    /**
     * 使用 Caffeine 缓存存储统计数据（5分钟 TTL，最大 100 条，自动清理过期条目）
     */
    private final Cache<String, StorageStats> statsCache = Caffeine.newBuilder()
            .expireAfterWrite(Constants.Cache.STATS_CACHE_TTL_MINUTES, TimeUnit.MINUTES)
            .maximumSize(Constants.Cache.STATS_CACHE_MAX_SIZE)
            .build();

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
                    space.setUrlPrefix(spaceConfig.getUrlPrefix());
                    space.setAllowAnonymous(spaceConfig.isAllowAnonymous());
                    space.setWatermark(spaceConfig.getWatermark());
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
                space.setUrlPrefix(spaceConfig.getUrlPrefix());
                space.setAllowAnonymous(spaceConfig.isAllowAnonymous());
                space.setWatermark(spaceConfig.getWatermark());
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
     * Validate URL prefix format
     */
    private boolean isValidUrlPrefix(String urlPrefix) {
        if (urlPrefix == null || urlPrefix.trim().isEmpty()) {
            return false;
        }
        // Must start with http:// or https://
        if (!urlPrefix.matches("^https?://.*")) {
            return false;
        }
        try {
            new java.net.URL(urlPrefix);
            return true;
        } catch (java.net.MalformedURLException e) {
            return false;
        }
    }

    /**
     * Create storage space (without watermark)
     */
    public boolean createStorageSpace(String name, String path, String maxSize, String urlPrefix, boolean allowAnonymous) {
        return createStorageSpace(name, path, maxSize, urlPrefix, allowAnonymous, null);
    }

    /**
     * Create storage space (with watermark)
     */
    public boolean createStorageSpace(String name, String path, String maxSize, String urlPrefix, boolean allowAnonymous, WatermarkConfig watermark) {
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

        // Validate URL prefix
        if (!isValidUrlPrefix(urlPrefix)) {
            logger.warn("Invalid URL prefix: {}", urlPrefix);
            return false;
        }

        // Validate path and create directory if not exists
        if (path == null || path.trim().isEmpty()) {
            logger.warn("Path is empty");
            return false;
        }

        File storageDir = new File(path);
        if (!storageDir.exists()) {
            if (!storageDir.mkdirs()) {
                logger.error("Failed to create storage directory: {}", path);
                return false;
            }
            logger.info("Created storage directory: {}", path);
        }

        SystemConfig.StorageSpace spaceConfig = new SystemConfig.StorageSpace();
        spaceConfig.setName(name);
        spaceConfig.setPath(path);
        spaceConfig.setMaxSize(maxSize);
        spaceConfig.setUrlPrefix(urlPrefix);
        spaceConfig.setAllowAnonymous(allowAnonymous);
        spaceConfig.setWatermark(watermark);

        if (config.getStorageSpaces() == null) {
            config.setStorageSpaces(new ArrayList<>());
        }
        config.getStorageSpaces().add(spaceConfig);

        // Automatically assign new storage space to all admin users
        if (config.getUsers() != null) {
            for (SystemConfig.User user : config.getUsers()) {
                if (Constants.Roles.ADMIN.equals(user.getRole()) && !user.getStorageSpaces().contains(name)) {
                    user.getStorageSpaces().add(name);
                    logger.info("Automatically assigned storage space {} to admin user {}", name, user.getUsername());
                }
            }
        }

        configService.saveConfig(config);

        // Clear cache after creating storage space
        clearStatsCache(name);

        logger.info("Storage space {} created", name);
        return true;
    }

    /**
     * Update storage space (without watermark)
     */
    public boolean updateStorageSpace(String name, String path, String maxSize, String urlPrefix, boolean allowAnonymous) {
        return updateStorageSpace(name, path, maxSize, urlPrefix, allowAnonymous, null);
    }

    /**
     * Update storage space (with watermark)
     */
    public boolean updateStorageSpace(String name, String path, String maxSize, String urlPrefix, boolean allowAnonymous, WatermarkConfig watermark) {
        SystemConfig config = configService.getConfig();
        if (config == null || config.getStorageSpaces() == null) {
            return false;
        }

        // Validate URL prefix
        if (!isValidUrlPrefix(urlPrefix)) {
            logger.warn("Invalid URL prefix: {}", urlPrefix);
            return false;
        }

        // Validate path and create directory if not exists
        if (path == null || path.trim().isEmpty()) {
            logger.warn("Path is empty");
            return false;
        }

        File storageDir = new File(path);
        if (!storageDir.exists()) {
            if (!storageDir.mkdirs()) {
                logger.error("Failed to create storage directory: {}", path);
                return false;
            }
            logger.info("Created storage directory: {}", path);
        }

        for (SystemConfig.StorageSpace spaceConfig : config.getStorageSpaces()) {
            if (spaceConfig.getName().equals(name)) {
                spaceConfig.setPath(path);
                spaceConfig.setMaxSize(maxSize);
                spaceConfig.setUrlPrefix(urlPrefix);
                spaceConfig.setAllowAnonymous(allowAnonymous);
                spaceConfig.setWatermark(watermark);

                configService.saveConfig(config);
                logger.info("Storage space {} updated", name);

                // Clear cache after updating storage space
                clearStatsCache(name);

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
                statsCache.invalidate(name);

                return true;
            }
        }

        return false;
    }

    /**
     * Get storage statistics
     */
    public StorageStats getStorageStats(String name) {
        // Try to get from cache first
        StorageStats cached = statsCache.getIfPresent(name);
        if (cached != null) {
            return cached;
        }

        // Cache miss - calculate and cache
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

        // Update cache (will automatically expire after 5 minutes)
        statsCache.put(name, stats);

        return stats;
    }

    /**
     * Calculate directory size recursively
     */
    private long calculateDirectorySize(File dir) {
        return FileUtils.calculateDirectorySize(dir);
    }

    /**
     * Count images and directories
     */
    private int[] countImagesAndDirectories(File dir) {
        return FileUtils.countFiles(dir, FileUtils::isImageFile,
                name -> name.equals(Constants.Directories.THUMBNAILS)
                        || name.equals(Constants.Directories.WATERMARKS));
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
            statsCache.invalidate(name);
        } else {
            statsCache.invalidateAll();
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