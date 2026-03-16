package com.wwh.simplepic.util;

import com.wwh.simplepic.model.StorageSpace;
import com.wwh.simplepic.model.SystemConfig;
import com.wwh.simplepic.service.ConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Storage utility class for storage space operations
 * 存储工具类 - 用于存储空间操作
 */
@Component
public class StorageUtils {

    @Autowired
    private ConfigService configService;

    /**
     * Find first storage space that allows anonymous upload
     * 查找第一个允许匿名上传的存储空间
     *
     * @return storage space name, or null if none found
     */
    public String findAnonymousUploadSpace() {
        SystemConfig config = configService.getConfig();
        if (config != null && config.getStorageSpaces() != null) {
            for (SystemConfig.StorageSpace space : config.getStorageSpaces()) {
                if (space.isAllowAnonymous()) {
                    return space.getName();
                }
            }
        }
        return null;
    }

    /**
     * Check if a specific storage space allows anonymous upload
     * 检查特定存储空间是否允许匿名上传
     *
     * @param storageSpaceName the name of the storage space
     * @return true if anonymous upload is allowed, false otherwise
     */
    public boolean isAnonymousUploadAllowed(String storageSpaceName) {
        SystemConfig config = configService.getConfig();
        if (config != null && config.getStorageSpaces() != null) {
            for (SystemConfig.StorageSpace space : config.getStorageSpaces()) {
                if (space.getName().equals(storageSpaceName) && space.isAllowAnonymous()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Get list of all storage space names
     * 获取所有存储空间名称列表
     *
     * @return list of storage space names
     */
    public List<String> getStorageSpaceNames() {
        List<String> names = new ArrayList<>();
        SystemConfig config = configService.getConfig();
        if (config != null && config.getStorageSpaces() != null) {
            for (SystemConfig.StorageSpace space : config.getStorageSpaces()) {
                names.add(space.getName());
            }
        }
        return names;
    }

    /**
     * Check if storage space exists
     * 检查存储空间是否存在
     *
     * @param storageSpaceName the name of the storage space
     * @return true if exists, false otherwise
     */
    public boolean storageSpaceExists(String storageSpaceName) {
        SystemConfig config = configService.getConfig();
        if (config != null && config.getStorageSpaces() != null) {
            for (SystemConfig.StorageSpace space : config.getStorageSpaces()) {
                if (space.getName().equals(storageSpaceName)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Get storage space path by name
     * 根据名称获取存储空间路径
     *
     * @param storageSpaceName the name of the storage space
     * @return the path, or null if not found
     */
    public String getStorageSpacePath(String storageSpaceName) {
        SystemConfig config = configService.getConfig();
        if (config != null && config.getStorageSpaces() != null) {
            for (SystemConfig.StorageSpace space : config.getStorageSpaces()) {
                if (space.getName().equals(storageSpaceName)) {
                    return space.getPath();
                }
            }
        }
        return null;
    }

    /**
     * Get storage space domain by name
     * 根据名称获取存储空间域名
     *
     * @param storageSpaceName the name of the storage space
     * @return the domain, or null if not found
     */
    public String getStorageSpaceDomain(String storageSpaceName) {
        SystemConfig config = configService.getConfig();
        if (config != null && config.getStorageSpaces() != null) {
            for (SystemConfig.StorageSpace space : config.getStorageSpaces()) {
                if (space.getName().equals(storageSpaceName)) {
                    return space.getUrlPrefix();
                }
            }
        }
        return null;
    }
}
