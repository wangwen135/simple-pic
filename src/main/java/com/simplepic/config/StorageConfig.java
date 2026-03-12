package com.simplepic.config;

import com.simplepic.model.StorageSpace;
import com.simplepic.model.SystemConfig;
import com.simplepic.service.ConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Storage configuration
 * 存储配置
 */
@Configuration
public class StorageConfig {

    @Autowired
    private ConfigService configService;

    @Bean
    public Map<String, StorageSpace> storageSpaceMap() {
        Map<String, StorageSpace> spaceMap = new HashMap<>();
        try {
            SystemConfig config = configService.getConfig();
            if (config != null && config.getStorageSpaces() != null) {
                for (SystemConfig.StorageSpace spaceConfig : config.getStorageSpaces()) {
                    StorageSpace space = new StorageSpace();
                    space.setName(spaceConfig.getName());
                    space.setPath(spaceConfig.getPath());
                    space.setMaxSize(spaceConfig.getMaxSize());
                    space.setDomain(spaceConfig.getDomain());
                    spaceMap.put(space.getName(), space);

                    // Create storage directories
                    File storageDir = new File(space.getPath());
                    if (!storageDir.exists()) {
                        storageDir.mkdirs();
                    }

                    // Create thumbnails directory
                    File thumbnailsDir = new File(storageDir, ".thumbnails");
                    if (!thumbnailsDir.exists()) {
                        thumbnailsDir.mkdirs();
                    }
                }
            }
        } catch (Exception e) {
            // Config file might not exist yet during first startup
        }
        return spaceMap;
    }
}