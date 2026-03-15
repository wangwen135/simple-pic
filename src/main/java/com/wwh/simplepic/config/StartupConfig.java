package com.wwh.simplepic.config;

import com.wwh.simplepic.model.SystemConfig;
import com.wwh.simplepic.service.ConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.io.File;
import java.security.SecureRandom;
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
     * Generate a cryptographically secure random password
     * 生成加密安全的随机密码
     */
    private String generateRandomPassword(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
        StringBuilder password = new StringBuilder();
        SecureRandom random = new SecureRandom();
        for (int i = 0; i < length; i++) {
            int index = random.nextInt(chars.length());
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

        // Frontend config - 默认使用明亮主题
        config.setTheme("light");
        config.setItemsPerPage(50);

        // 不创建默认存储空间，需要用户手动配置

        // 只创建 admin 用户
        SystemConfig.User adminUser = new SystemConfig.User();
        adminUser.setUsername("admin");
        adminUser.setPassword(passwordEncoder.encode(adminPassword));
        adminUser.setRole("ADMIN");
        // 不分配存储空间，等用户创建后再分配
        config.getUsers().add(adminUser);

        // 水印默认关闭
        config.setWatermarkEnabled(false);
        config.setWatermarkType("text");
        config.setWatermarkContent("");
        config.setWatermarkPosition("bottom-right");
        config.setWatermarkOpacity(0.5);

        // Security config
        config.setRateLimitEnabled(true);
        config.setMaxRequests(100);
        config.setTimeWindow(60);

        // Login lockout config
        config.setLoginLockoutEnabled(true);
        config.setMaxFailedAttempts(5);
        config.setLockoutMinutes(10);

        return config;
    }
}