package com.wwh.simplepic.config;

import com.wwh.simplepic.model.StorageSpace;
import com.wwh.simplepic.model.SystemConfig;
import com.wwh.simplepic.service.ConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
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
                    space.setUrlPrefix(spaceConfig.getUrlPrefix());
                    space.setAllowAnonymous(spaceConfig.isAllowAnonymous());
                    spaceMap.put(space.getName(), space);

                    // Create storage directories
                    File storageDir = new File(space.getPath());
                    if (!storageDir.exists()) {
                        storageDir.mkdirs();
                    }
                }
            }
        } catch (Exception e) {
            // 首次启动时配置文件可能不存在，属于正常情况
        }
        return spaceMap;
    }
}