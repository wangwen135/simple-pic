package com.simplepic.service;

import com.simplepic.model.SystemConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Configuration service
 * 配置服务
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
     * Load configuration from file
     * 从文件加载配置
     */
    private void loadConfig() {
        File configFile = getConfigFile();

        if (!configFile.exists()) {
            logger.warn("Configuration file not found: {}", configFile.getAbsolutePath());
            return;
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(configFile), StandardCharsets.UTF_8))) {
            List<String> lines = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }

            configCache = parseYaml(lines);
            configLastModified = configFile.lastModified();

            logger.info("Configuration loaded from: {}", configFile.getAbsolutePath());

            // Log user count only (no sensitive data)
            if (configCache.getUsers() != null && !configCache.getUsers().isEmpty()) {
                logger.info("Loaded {} user(s)", configCache.getUsers().size());
            }
        } catch (IOException e) {
            logger.error("Failed to load configuration", e);
        }
    }

    /**
     * Parse YAML configuration from list of lines
     * 从行列表解析YAML配置
     * Custom YAML parser to handle the configuration structure
     * 自定义YAML解析器，用于处理配置结构
     *
     * @param lines list of YAML lines
     * @return parsed SystemConfig object
     */
    private SystemConfig parseYaml(List<String> lines) {
        SystemConfig config = new SystemConfig();
        List<SystemConfig.StorageSpace> storageSpaces = new ArrayList<>();
        List<SystemConfig.User> users = new ArrayList<>();
        List<SystemConfig.ApiKey> apiKeys = new ArrayList<>();

        String section = "";
        int indentLevel = 0;
        SystemConfig.StorageSpace currentSpace = null;
        SystemConfig.User currentUser = null;

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            String trimmed = line.trim();
            int indent = getIndent(line);

            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }

            // Handle section changes based on indent
            if (indent < 4 && (trimmed.endsWith(":") || trimmed.startsWith("- "))) {
                // Top-level section
                if (trimmed.startsWith("simple-pic:")) {
                    section = "simple-pic";
                    indentLevel = 0;
                } else if (trimmed.startsWith("system:") || (indent == 2 && trimmed.startsWith("system:"))) {
                    section = "system";
                } else if (trimmed.startsWith("storage-spaces:") || (indent == 2 && trimmed.startsWith("storage-spaces:"))) {
                    section = "storage-spaces";
                } else if (trimmed.startsWith("users:")) {
                    section = "users";
                    currentUser = null;
                } else if (trimmed.startsWith("api-keys:")) {
                    section = "api-keys";
                } else if (trimmed.startsWith("watermark:")) {
                    section = "watermark";
                } else if (trimmed.startsWith("security:")) {
                    section = "security";
                } else if (trimmed.startsWith("frontend:")) {
                    section = "frontend";
                } else if (trimmed.startsWith("login-lockout:")) {
                    section = "login-lockout";
                }
                continue;
            }

            // Parse storage space item
            if (section.equals("storage-spaces") && indent == 4 && trimmed.startsWith("- name:")) {
                currentSpace = new SystemConfig.StorageSpace();
                currentSpace.setName(trimmed.substring(7).trim());
                storageSpaces.add(currentSpace);
                continue;
            }

            // Parse storage space properties
            if (section.equals("storage-spaces") && indent == 6 && currentSpace != null) {
                if (trimmed.startsWith("path:")) {
                    currentSpace.setPath(trimmed.substring(5).trim());
                } else if (trimmed.startsWith("max-size:") || trimmed.startsWith("maxSize:")) {
                    currentSpace.setMaxSize(trimmed.substring(trimmed.indexOf(":") + 1).trim());
                } else if (trimmed.startsWith("domain:")) {
                    currentSpace.setDomain(trimmed.substring(7).trim());
                } else if (trimmed.startsWith("allow-anonymous:") || trimmed.startsWith("allowAnonymous:")) {
                    currentSpace.setAllowAnonymous(Boolean.parseBoolean(trimmed.substring(trimmed.indexOf(":") + 1).trim()));
                }
                continue;
            }

            // Parse user item
            if (section.equals("users") && indent == 4 && trimmed.startsWith("- username:")) {
                currentUser = new SystemConfig.User();
                currentUser.setUsername(trimmed.substring(11).trim());
                users.add(currentUser);
                logger.debug("Created user: {}", currentUser.getUsername());
                continue;
            }

            // Parse user properties
            if (section.equals("users") && indent == 6 && currentUser != null) {
                if (trimmed.startsWith("password:")) {
                    String password = trimmed.substring(9).trim();
                    currentUser.setPassword(password);
                    logger.debug("Password set for user: {}", currentUser.getUsername());
                } else if (trimmed.startsWith("role:")) {
                    currentUser.setRole(trimmed.substring(5).trim());
                } else if (trimmed.startsWith("storage-spaces:") || trimmed.startsWith("storageSpaces:")) {
                    // Parse [default]
                    String listStr = trimmed.substring(trimmed.indexOf("[") + 1, trimmed.indexOf("]")).trim();
                    String[] spaces = listStr.split(",");
                    for (String space : spaces) {
                        if (!space.trim().isEmpty()) {
                            currentUser.getStorageSpaces().add(space.trim());
                        }
                    }
                }
                continue;
            }

            // Parse API key item
            if (section.equals("api-keys") && indent == 4 && trimmed.startsWith("- token:")) {
                SystemConfig.ApiKey apiKey = new SystemConfig.ApiKey();
                apiKey.setToken(trimmed.substring(7).trim());
                apiKeys.add(apiKey);
                continue;
            }

            // Parse API key properties
            if (section.equals("api-keys") && indent == 6) {
                if (trimmed.startsWith("storage-space:") || trimmed.startsWith("storageSpace:")) {
                    if (!apiKeys.isEmpty()) {
                        apiKeys.get(apiKeys.size() - 1).setStorageSpace(trimmed.substring(trimmed.indexOf(":") + 1).trim());
                    }
                }
                continue;
            }

            // Parse system properties
            if (section.equals("system") && indent == 4) {
                if (trimmed.startsWith("name:")) {
                    config.setName(trimmed.substring(5).trim());
                } else if (trimmed.startsWith("description:")) {
                    config.setDescription(trimmed.substring(12).trim());
                } else if (trimmed.startsWith("anonymous-upload-enabled:") || trimmed.startsWith("anonymousUploadEnabled:")) {
                    config.setAnonymousUploadEnabled(Boolean.parseBoolean(trimmed.substring(trimmed.indexOf(":") + 1).trim()));
                } else if (trimmed.startsWith("allowed-origins:") || trimmed.startsWith("allowedOrigins:")) {
                    config.setAllowedOrigins(trimmed.substring(trimmed.indexOf(":") + 1).trim());
                }
                continue;
            }

            // Parse watermark properties
            if (section.equals("watermark") && indent == 4) {
                if (trimmed.startsWith("enabled:")) {
                    config.setWatermarkEnabled(Boolean.parseBoolean(trimmed.substring(8).trim()));
                } else if (trimmed.startsWith("type:")) {
                    config.setWatermarkType(trimmed.substring(5).trim());
                } else if (trimmed.startsWith("content:")) {
                    config.setWatermarkContent(trimmed.substring(8).trim());
                } else if (trimmed.startsWith("position:")) {
                    config.setWatermarkPosition(trimmed.substring(9).trim());
                } else if (trimmed.startsWith("opacity:")) {
                    config.setWatermarkOpacity(Double.parseDouble(trimmed.substring(8).trim()));
                }
                continue;
            }

            // Parse security properties
            if (section.equals("security") && indent == 4) {
                if (trimmed.startsWith("rate-limit:")) {
                    continue;
                }
            }

            // Parse rate-limit properties
            if (section.equals("rate-limit") && indent == 6) {
                if (trimmed.startsWith("enabled:")) {
                    config.setRateLimitEnabled(Boolean.parseBoolean(trimmed.substring(8).trim()));
                } else if (trimmed.startsWith("max-requests:") || trimmed.startsWith("maxRequests:")) {
                    config.setMaxRequests(Integer.parseInt(trimmed.substring(trimmed.indexOf(":") + 1).trim()));
                } else if (trimmed.startsWith("time-window:") || trimmed.startsWith("timeWindow:")) {
                    config.setTimeWindow(Integer.parseInt(trimmed.substring(trimmed.indexOf(":") + 1).trim()));
                }
                continue;
            }

            // Parse frontend properties
            if (section.equals("frontend") && indent == 4) {
                if (trimmed.startsWith("theme:")) {
                    config.setTheme(trimmed.substring(trimmed.indexOf(":") + 1).trim());
                } else if (trimmed.startsWith("items-per-page:")) {
                    config.setItemsPerPage(Integer.parseInt(trimmed.substring(trimmed.indexOf(":") + 1).trim()));
                }
                continue;
            }

            // Parse login-lockout properties
            if (section.equals("login-lockout") && indent == 6) {
                if (trimmed.startsWith("enabled:")) {
                    config.setLoginLockoutEnabled(Boolean.parseBoolean(trimmed.substring(8).trim()));
                } else if (trimmed.startsWith("max-failed-attempts:") || trimmed.startsWith("maxFailedAttempts:")) {
                    config.setMaxFailedAttempts(Integer.parseInt(trimmed.substring(trimmed.indexOf(":") + 1).trim()));
                } else if (trimmed.startsWith("lockout-minutes:") || trimmed.startsWith("lockoutMinutes:")) {
                    config.setLockoutMinutes(Integer.parseInt(trimmed.substring(trimmed.indexOf(":") + 1).trim()));
                }
                continue;
            }
        }

        config.setStorageSpaces(storageSpaces);
        config.setUsers(users);
        config.setApiKeys(apiKeys);

        // Set defaults
        if (config.getTheme() == null || config.getTheme().isEmpty()) config.setTheme("light");
        if (config.getItemsPerPage() == 0) config.setItemsPerPage(50);

        return config;
    }

    private int getIndent(String line) {
        int count = 0;
        while (count < line.length() && line.charAt(count) == ' ') {
            count++;
        }
        return count;
    }

    /**
     * Save configuration to file
     * 保存配置到文件
     */
    public void saveConfig(SystemConfig config) {
        this.configCache = config;

        File configFile = getConfigFile();

        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(configFile), StandardCharsets.UTF_8))) {
            // Write YAML content (no BOM - BOM can cause parsing issues)
            writer.write(generateYaml(config));
            configLastModified = System.currentTimeMillis();

            logger.info("Configuration saved to: {}", configFile.getAbsolutePath());
        } catch (IOException e) {
            logger.error("Failed to save configuration", e);
        }
    }

    /**
     * Generate YAML configuration from SystemConfig object
     * 从SystemConfig对象生成YAML配置
     * Custom YAML generator to handle the configuration structure
     * 自定义YAML生成器，用于处理配置结构
     *
     * @param config the SystemConfig object
     * @return YAML string representation
     */
    private String generateYaml(SystemConfig config) {
        StringBuilder yaml = new StringBuilder();
        yaml.append("simple-pic:\n");
        yaml.append("  system:\n");
        yaml.append("    name: ").append(config.getName()).append("\n");
        yaml.append("    description: ").append(config.getDescription()).append("\n");
        yaml.append("    anonymous-upload-enabled: ").append(config.isAnonymousUploadEnabled()).append("\n");
        if (config.getAllowedOrigins() != null && !config.getAllowedOrigins().isEmpty()) {
            yaml.append("    allowed-origins: ").append(config.getAllowedOrigins()).append("\n");
        }
        yaml.append("  storage-spaces:\n");

        if (config.getStorageSpaces() != null) {
            for (SystemConfig.StorageSpace space : config.getStorageSpaces()) {
                yaml.append("    - name: ").append(space.getName()).append("\n");
                yaml.append("      path: ").append(space.getPath()).append("\n");
                yaml.append("      max-size: ").append(space.getMaxSize()).append("\n");
                yaml.append("      domain: ").append(space.getDomain()).append("\n");
                yaml.append("      allow-anonymous: ").append(space.isAllowAnonymous()).append("\n");
            }
        }

        yaml.append("  users:\n");
        if (config.getUsers() != null) {
            for (SystemConfig.User user : config.getUsers()) {
                yaml.append("    - username: ").append(user.getUsername()).append("\n");
                yaml.append("      password: ").append(user.getPassword()).append("\n");
                yaml.append("      role: ").append(user.getRole()).append("\n");
                yaml.append("      storage-spaces: [").append(String.join(", ", user.getStorageSpaces())).append("]\n");
            }
        }

        yaml.append("  api-keys:\n");
        if (config.getApiKeys() != null) {
            for (SystemConfig.ApiKey key : config.getApiKeys()) {
                yaml.append("    - token: ").append(key.getToken()).append("\n");
                yaml.append("      storage-space: ").append(key.getStorageSpace()).append("\n");
            }
        }

        yaml.append("  watermark:\n");
        yaml.append("    enabled: ").append(config.isWatermarkEnabled()).append("\n");
        yaml.append("    type: ").append(config.getWatermarkType()).append("\n");
        yaml.append("    content: ").append(config.getWatermarkContent()).append("\n");
        yaml.append("    position: ").append(config.getWatermarkPosition()).append("\n");
        yaml.append("    opacity: ").append(config.getWatermarkOpacity()).append("\n");

        yaml.append("  security:\n");
        yaml.append("    rate-limit:\n");
        yaml.append("      enabled: ").append(config.isRateLimitEnabled()).append("\n");
        yaml.append("      max-requests: ").append(config.getMaxRequests()).append("\n");
        yaml.append("      time-window: ").append(config.getTimeWindow()).append("\n");

        yaml.append("  frontend:\n");
        yaml.append("    theme: ").append(config.getTheme()).append("\n");
        yaml.append("    items-per-page: ").append(config.getItemsPerPage()).append("\n");

        yaml.append("  login-lockout:\n");
        yaml.append("    enabled: ").append(config.isLoginLockoutEnabled()).append("\n");
        yaml.append("    max-failed-attempts: ").append(config.getMaxFailedAttempts()).append("\n");
        yaml.append("    lockout-minutes: ").append(config.getLockoutMinutes()).append("\n");

        return yaml.toString();
    }

    public void reload() {
        loadConfig();
    }
}