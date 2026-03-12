package com.simplepic.config;

import com.simplepic.model.SystemConfig;
import com.simplepic.service.ConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.UUID;

/**
 * Startup configuration - runs on application start
 * 启动配置 - 应用启动时运行
 */
@Component
public class StartupConfig implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(StartupConfig.class);

    @Value("${spring.application.name:Simple-Pic}")
    private String appName;

    @Autowired
    private ConfigService configService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // Check if config.yml exists
        File configFile = configService.getConfigFile();

        if (!configFile.exists()) {
            logger.info("==============================================");
            logger.info("{} - First startup detected", appName);
            logger.info("==============================================");

            // Generate random password for admin
            String adminPassword = generateRandomPassword(12);

            // Create default configuration
            SystemConfig defaultConfig = createDefaultConfig(adminPassword);

            // Save configuration
            configService.saveConfig(defaultConfig);

            logger.info("");
            logger.info("Default configuration created at: {}", configFile.getAbsolutePath());
            logger.info("");
            logger.info("==============================================");
            logger.info("ADMIN CREDENTIALS:");
            logger.info("Username: admin");
            logger.info("Password: {}", adminPassword);
            logger.info("==============================================");
            logger.info("");
            logger.info("Please login to the admin panel to configure the system");
            logger.info("Admin panel: http://localhost:8080/admin/dashboard.html");
            logger.info("");
            logger.info("==============================================");
        } else {
            logger.info("Configuration file found: {}", configFile.getAbsolutePath());
        }
    }

    /**
     * Generate a random password
     */
    private String generateRandomPassword(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
        StringBuilder password = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int index = (int) (Math.random() * chars.length());
            password.append(chars.charAt(index));
        }
        return password.toString();
    }

    /**
     * Create default system configuration
     */
    private SystemConfig createDefaultConfig(String adminPassword) {
        SystemConfig config = new SystemConfig();

        // System info
        config.setName("Simple-Pic");
        config.setDescription("简单好用的本地图床");

        // Frontend config
        config.setTheme("dark");
        config.setItemsPerPage(50);

        // Create default storage space
        SystemConfig.StorageSpace defaultStorage = new SystemConfig.StorageSpace();
        defaultStorage.setName("default");
        defaultStorage.setPath("./storage/default");
        defaultStorage.setMaxSize("10GB");
        defaultStorage.setDomain("http://localhost:8080");
        config.getStorageSpaces().add(defaultStorage);

        // Create admin user
        SystemConfig.User adminUser = new SystemConfig.User();
        adminUser.setUsername("admin");
        adminUser.setPassword(passwordEncoder.encode(adminPassword));
        adminUser.setRole("ADMIN");
        adminUser.getStorageSpaces().add("default");
        config.getUsers().add(adminUser);

        // Create default user
        SystemConfig.User defaultUser = new SystemConfig.User();
        defaultUser.setUsername("user");
        defaultUser.setPassword(passwordEncoder.encode("user123"));
        defaultUser.setRole("USER");
        defaultUser.getStorageSpaces().add("default");
        config.getUsers().add(defaultUser);

        // Watermark config
        config.setWatermarkEnabled(true);
        config.setWatermarkType("text");
        config.setWatermarkContent("Simple-Pic");
        config.setWatermarkPosition("bottom-right");
        config.setWatermarkOpacity(0.5);

        // Security config
        config.setRateLimitEnabled(true);
        config.setMaxRequests(100);
        config.setTimeWindow(60);

        return config;
    }
}