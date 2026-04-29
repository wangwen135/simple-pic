package com.wwh.simplepic.service;

import com.wwh.simplepic.model.WatermarkConfig;
import com.wwh.simplepic.model.SystemConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.introspector.PropertyUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Configuration service using SnakeYAML
 * 配置服务 - 使用 SnakeYAML 解析
 */
@Service
public class ConfigService {

    private static final Logger logger = LoggerFactory.getLogger(ConfigService.class);

    private static final String CONFIG_FILE = "config.yml";

    private SystemConfig configCache;
    private long configLastModified = 0;

    @Value("${spring.application.name:Simple-Pic}")
    private String appName;

    public File getConfigFile() {
        return new File(CONFIG_FILE);
    }

    public SystemConfig getConfig() {
        File configFile = getConfigFile();

        if (configFile.exists() && (configCache == null || configFile.lastModified() > configLastModified)) {
            loadConfig();
        }

        return configCache;
    }

    /**
     * Create YAML instance for parsing and dumping
     */
    private Yaml createYaml() {
        DumperOptions dumperOptions = new DumperOptions();
        dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        dumperOptions.setPrettyFlow(true);
        dumperOptions.setIndent(2);
        dumperOptions.setWidth(120);

        return new Yaml(dumperOptions);
    }

    /**
     * Load configuration from file
     * 从文件加载配置
     */
    private void loadConfig() {
        File configFile = getConfigFile();

        if (!configFile.exists()) {
            logger.warn("Configuration file not found: {}", configFile.getAbsolutePath());
            return;
        }

        try (InputStream inputStream = new FileInputStream(configFile)) {
            Yaml yaml = createYaml();

            // Parse YAML to map
            Map<String, Object> data = yaml.load(inputStream);

            if (data != null && data.containsKey("simple-pic")) {
                Map<String, Object> simplePicData = (Map<String, Object>) data.get("simple-pic");
                configCache = parseFromMap(simplePicData);
            } else {
                // Legacy format support
                configCache = parseFromMap(data);
            }

            configLastModified = configFile.lastModified();

            logger.info("Configuration loaded from: {}", configFile.getAbsolutePath());

            // Log user count only (no sensitive data)
            if (configCache.getUsers() != null && !configCache.getUsers().isEmpty()) {
                logger.info("Loaded {} user(s)", configCache.getUsers().size());
            }
        } catch (Exception e) {
            logger.error("Failed to load configuration", e);
        }
    }

    /**
     * Parse configuration from map (SnakeYAML parsed structure)
     */
    @SuppressWarnings("unchecked")
    private SystemConfig parseFromMap(Map<String, Object> data) {
        SystemConfig config = new SystemConfig();

        // Parse system section
        Map<String, Object> systemData = (Map<String, Object>) data.get("system");
        if (systemData != null) {
            config.setName(getStringValue(systemData, "name", "Simple-Pic"));
            config.setDescription(getStringValue(systemData, "description", ""));
            config.setAnonymousUploadEnabled(getBooleanValue(systemData, "anonymous-upload-enabled", false));
            config.setWatermarkEnabled(getBooleanValue(systemData, "watermark-enabled", false));
            config.setMaxFileSizeMB(getIntValue(systemData, "max-file-size-mb", 10));
            config.setAllowedFileTypes(getStringValue(systemData, "allowed-file-types", "jpg,jpeg,png,gif,webp,svg"));
        }

        // Parse storage spaces
        List<Map<String, Object>> storageSpacesData = (List<Map<String, Object>>) data.get("storage-spaces");
        if (storageSpacesData != null) {
            List<SystemConfig.StorageSpace> storageSpaces = new ArrayList<>();
            for (Map<String, Object> spaceData : storageSpacesData) {
                SystemConfig.StorageSpace space = new SystemConfig.StorageSpace();
                space.setName(getStringValue(spaceData, "name", ""));
                space.setPath(getStringValue(spaceData, "path", ""));
                space.setMaxSize(getStringValue(spaceData, "max-size", "10GB"));
                space.setUrlPrefix(getStringValue(spaceData, "url-prefix", ""));
                space.setAllowAnonymous(getBooleanValue(spaceData, "allow-anonymous", false));

                // 解析每个存储空间的水印配置
                Map<String, Object> wmData = (Map<String, Object>) spaceData.get("watermark");
                if (wmData != null) {
                    WatermarkConfig wm = new WatermarkConfig();
                    wm.setEnabled(getBooleanValue(wmData, "enabled", false));
                    wm.setType(getStringValue(wmData, "type", "text"));
                    wm.setContent(getStringValue(wmData, "content", ""));
                    wm.setPosition(getStringValue(wmData, "position", "bottom-right"));
                    wm.setOpacity(getDoubleValue(wmData, "opacity", 0.5));
                    space.setWatermark(wm);
                }

                // Legacy support: domain field
                if (spaceData.containsKey("domain") && !spaceData.containsKey("url-prefix")) {
                    space.setUrlPrefix(getStringValue(spaceData, "domain", ""));
                }

                storageSpaces.add(space);
            }
            config.setStorageSpaces(storageSpaces);
        }

        // Parse users
        List<Map<String, Object>> usersData = (List<Map<String, Object>>) data.get("users");
        if (usersData != null) {
            List<SystemConfig.User> users = new ArrayList<>();
            for (Map<String, Object> userData : usersData) {
                SystemConfig.User user = new SystemConfig.User();
                user.setUsername(getStringValue(userData, "username", ""));
                user.setPassword(getStringValue(userData, "password", ""));
                user.setRole(getStringValue(userData, "role", "USER"));

                // Parse storage spaces list
                Object storageSpacesObj = userData.get("storage-spaces");
                if (storageSpacesObj instanceof List) {
                    for (String space : (List<String>) storageSpacesObj) {
                        user.getStorageSpaces().add(space);
                    }
                } else if (storageSpacesObj instanceof String) {
                    String listStr = (String) storageSpacesObj;
                    if (listStr.startsWith("[") && listStr.endsWith("]")) {
                        String[] spaces = listStr.substring(1, listStr.length() - 1).split(",");
                        for (String space : spaces) {
                            if (!space.trim().isEmpty()) {
                                user.getStorageSpaces().add(space.trim());
                            }
                        }
                    }
                }

                users.add(user);
            }
            config.setUsers(users);
        }

        // Parse API keys
        List<Map<String, Object>> apiKeysData = (List<Map<String, Object>>) data.get("api-keys");
        if (apiKeysData != null) {
            List<SystemConfig.ApiKey> apiKeys = new ArrayList<>();
            for (Map<String, Object> keyData : apiKeysData) {
                SystemConfig.ApiKey apiKey = new SystemConfig.ApiKey();
                apiKey.setToken(getStringValue(keyData, "token", ""));
                apiKey.setStorageSpace(getStringValue(keyData, "storage-space", ""));
                apiKeys.add(apiKey);
            }
            config.setApiKeys(apiKeys);
        }

        // Parse security/rate-limit
        Map<String, Object> securityData = (Map<String, Object>) data.get("security");
        if (securityData != null) {
            Map<String, Object> rateLimitData = (Map<String, Object>) securityData.get("rate-limit");
            if (rateLimitData != null) {
                config.setRateLimitEnabled(getBooleanValue(rateLimitData, "enabled", true));
                config.setMaxRequests(getIntValue(rateLimitData, "max-requests", 10));
                config.setTimeWindow(getIntValue(rateLimitData, "time-window", 60));
            }
        }

        // Parse login-lockout
        Map<String, Object> loginLockoutData = (Map<String, Object>) data.get("login-lockout");
        if (loginLockoutData != null) {
            config.setLoginLockoutEnabled(getBooleanValue(loginLockoutData, "enabled", false));
            config.setMaxFailedAttempts(getIntValue(loginLockoutData, "max-failed-attempts", 5));
            config.setLockoutMinutes(getIntValue(loginLockoutData, "lockout-minutes", 30));
        }

        // Parse hotlink-protection
        Map<String, Object> hotlinkData = (Map<String, Object>) data.get("hotlink-protection");
        if (hotlinkData != null) {
            config.setHotlinkProtectionEnabled(getBooleanValue(hotlinkData, "enabled", false));
            config.setAllowedReferers(getStringValue(hotlinkData, "allowed-referers", ""));
            config.setHotlinkResponse(getStringValue(hotlinkData, "response", "generated"));
            config.setHotlinkImagePath(getStringValue(hotlinkData, "image-path", ""));
        }

        // Set defaults
        return config;
    }

    private String getStringValue(Map<String, Object> map, String key, String defaultValue) {
        Object value = map.get(key);
        if (value == null) {
            // Try with camelCase
            String camelKey = key.replace("-", "");
            value = map.get(camelKey);
        }
        return value != null ? value.toString() : defaultValue;
    }

    private boolean getBooleanValue(Map<String, Object> map, String key, boolean defaultValue) {
        Object value = map.get(key);
        if (value == null) {
            String camelKey = key.replace("-", "");
            value = map.get(camelKey);
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return defaultValue;
    }

    private int getIntValue(Map<String, Object> map, String key, int defaultValue) {
        Object value = map.get(key);
        if (value == null) {
            String camelKey = key.replace("-", "");
            value = map.get(camelKey);
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }

    private double getDoubleValue(Map<String, Object> map, String key, double defaultValue) {
        Object value = map.get(key);
        if (value == null) {
            String camelKey = key.replace("-", "");
            value = map.get(camelKey);
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return defaultValue;
    }

    /**
     * Save configuration to file
     * 保存配置到文件
     */
    public void saveConfig(SystemConfig config) {
        this.configCache = config;

        File configFile = getConfigFile();

        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(configFile), StandardCharsets.UTF_8))) {
            Yaml yaml = createYaml();
            writer.write(yaml.dump(generateMap(config)));
            configLastModified = System.currentTimeMillis();

            logger.info("Configuration saved to: {}", configFile.getAbsolutePath());
        } catch (IOException e) {
            logger.error("Failed to save configuration", e);
        }
    }

    /**
     * Update system settings only (name, description, watermark, rate limit, etc.)
     * Preserves users, storage spaces, and API keys
     * 只更新系统设置（名称、描述、水印、限流等），保留用户、存储空间和 API 密钥
     */
    public void updateSystemSettings(SystemConfig newSettings) {
        SystemConfig currentConfig = getConfig();

        // Update only system settings fields
        currentConfig.setName(newSettings.getName());
        currentConfig.setDescription(newSettings.getDescription());
        currentConfig.setAnonymousUploadEnabled(newSettings.isAnonymousUploadEnabled());
        currentConfig.setWatermarkEnabled(newSettings.isWatermarkEnabled());
        currentConfig.setMaxFileSizeMB(newSettings.getMaxFileSizeMB());
        currentConfig.setAllowedFileTypes(newSettings.getAllowedFileTypes());

        // Security settings
        currentConfig.setRateLimitEnabled(newSettings.isRateLimitEnabled());
        currentConfig.setMaxRequests(newSettings.getMaxRequests());
        currentConfig.setTimeWindow(newSettings.getTimeWindow());

        // Login lockout settings
        currentConfig.setLoginLockoutEnabled(newSettings.isLoginLockoutEnabled());
        currentConfig.setMaxFailedAttempts(newSettings.getMaxFailedAttempts());
        currentConfig.setLockoutMinutes(newSettings.getLockoutMinutes());

        // Hotlink protection settings
        currentConfig.setHotlinkProtectionEnabled(newSettings.isHotlinkProtectionEnabled());
        currentConfig.setAllowedReferers(newSettings.getAllowedReferers());
        currentConfig.setHotlinkResponse(newSettings.getHotlinkResponse());
        // hotlinkImagePath is updated separately via upload endpoint

        // Save the updated config
        saveConfig(currentConfig);
    }

    /**
     * Generate map from SystemConfig for YAML dumping
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> generateMap(SystemConfig config) {
        Map<String, Object> simplePic = new LinkedHashMap<>();

        // System section
        Map<String, Object> system = new LinkedHashMap<>();
        system.put("name", config.getName());
        system.put("description", config.getDescription());
        system.put("anonymous-upload-enabled", config.isAnonymousUploadEnabled());
        system.put("watermark-enabled", config.isWatermarkEnabled());
        system.put("max-file-size-mb", config.getMaxFileSizeMB());
        system.put("allowed-file-types", config.getAllowedFileTypes());
        simplePic.put("system", system);

        // Storage spaces
        List<Map<String, Object>> storageSpaces = new ArrayList<>();
        if (config.getStorageSpaces() != null) {
            for (SystemConfig.StorageSpace space : config.getStorageSpaces()) {
                Map<String, Object> spaceMap = new LinkedHashMap<>();
                spaceMap.put("name", space.getName());
                spaceMap.put("path", space.getPath());
                spaceMap.put("max-size", space.getMaxSize());
                spaceMap.put("url-prefix", space.getUrlPrefix() != null ? space.getUrlPrefix() : "");
                spaceMap.put("allow-anonymous", space.isAllowAnonymous());
                if (space.getWatermark() != null) {
                    Map<String, Object> wmMap = new LinkedHashMap<>();
                    WatermarkConfig wm = space.getWatermark();
                    wmMap.put("enabled", wm.isEnabled());
                    wmMap.put("type", wm.getType());
                    wmMap.put("content", wm.getContent());
                    wmMap.put("position", wm.getPosition());
                    wmMap.put("opacity", wm.getOpacity());
                    spaceMap.put("watermark", wmMap);
                }
                storageSpaces.add(spaceMap);
            }
        }
        simplePic.put("storage-spaces", storageSpaces);

        // Users
        List<Map<String, Object>> users = new ArrayList<>();
        if (config.getUsers() != null) {
            for (SystemConfig.User user : config.getUsers()) {
                Map<String, Object> userMap = new LinkedHashMap<>();
                userMap.put("username", user.getUsername());
                userMap.put("password", user.getPassword());
                userMap.put("role", user.getRole());
                userMap.put("storage-spaces", user.getStorageSpaces());
                users.add(userMap);
            }
        }
        simplePic.put("users", users);

        // API keys
        List<Map<String, Object>> apiKeys = new ArrayList<>();
        if (config.getApiKeys() != null) {
            for (SystemConfig.ApiKey key : config.getApiKeys()) {
                Map<String, Object> keyMap = new LinkedHashMap<>();
                keyMap.put("token", key.getToken());
                keyMap.put("storage-space", key.getStorageSpace());
                apiKeys.add(keyMap);
            }
        }
        simplePic.put("api-keys", apiKeys);

        // Security / Rate limit
        Map<String, Object> security = new LinkedHashMap<>();
        Map<String, Object> rateLimit = new LinkedHashMap<>();
        rateLimit.put("enabled", config.isRateLimitEnabled());
        rateLimit.put("max-requests", config.getMaxRequests());
        rateLimit.put("time-window", config.getTimeWindow());
        security.put("rate-limit", rateLimit);
        simplePic.put("security", security);

        // Login lockout
        Map<String, Object> loginLockout = new LinkedHashMap<>();
        loginLockout.put("enabled", config.isLoginLockoutEnabled());
        loginLockout.put("max-failed-attempts", config.getMaxFailedAttempts());
        loginLockout.put("lockout-minutes", config.getLockoutMinutes());
        simplePic.put("login-lockout", loginLockout);

        // Hotlink protection
        Map<String, Object> hotlink = new LinkedHashMap<>();
        hotlink.put("enabled", config.isHotlinkProtectionEnabled());
        hotlink.put("allowed-referers", config.getAllowedReferers() != null ? config.getAllowedReferers() : "");
        hotlink.put("response", config.getHotlinkResponse() != null ? config.getHotlinkResponse() : "generated");
        if (config.getHotlinkImagePath() != null && !config.getHotlinkImagePath().isEmpty()) {
            hotlink.put("image-path", config.getHotlinkImagePath());
        }
        simplePic.put("hotlink-protection", hotlink);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("simple-pic", simplePic);
        return result;
    }

    public void reload() {
        loadConfig();
    }
}
