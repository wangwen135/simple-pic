package com.wwh.simplepic.controller;

import com.wwh.simplepic.model.*;
import com.wwh.simplepic.service.*;
import com.wwh.simplepic.util.ErrorMessages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.servlet.http.Cookie;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Admin controller
 * 管理员控制器
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    @Autowired
    private ImageService imageService;

    @Autowired
    private StorageService storageService;

    @Autowired
    private UserService userService;

    @Autowired
    private AuthService authService;

    @Autowired
    private ConfigService configService;

    @Autowired
    private ThumbnailService thumbnailService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Generate password hash (for testing)
     */
    @GetMapping("/gen-password")
    public ResponseEntity<Map<String, String>> generatePassword(@RequestParam String password) {
        String hash = passwordEncoder.encode(password);
        Map<String, String> result = new HashMap<>();
        result.put("password", password);
        result.put("hash", hash);
        return ResponseEntity.ok(result);
    }

    /**
     * Dashboard statistics
     */
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboard() {
        Map<String, Object> dashboard = new HashMap<>();

        // Storage stats
        List<StorageStats> storageStats = storageService.getAllStorageStats();
        dashboard.put("storageStats", storageStats);

        // Total images count
        int totalImages = storageStats.stream().mapToInt(StorageStats::getImageCount).sum();
        dashboard.put("totalImages", totalImages);

        // Total used space
        long totalUsed = storageStats.stream().mapToLong(StorageStats::getUsedSize).sum();
        dashboard.put("totalUsed", totalUsed);

        // Total space
        long totalSpace = storageStats.stream().mapToLong(StorageStats::getTotalSize).sum();
        dashboard.put("totalSpace", totalSpace);

        // Active sessions
        int activeSessions = authService.getActiveSessionCount();
        dashboard.put("activeSessions", activeSessions);

        // System config
        SystemConfig config = configService.getConfig();
        dashboard.put("systemName", config != null ? config.getName() : "Simple-Pic");
        dashboard.put("systemDescription", config != null ? config.getDescription() : "");

        return ResponseEntity.ok(dashboard);
    }

    /**
     * Get storage spaces
     */
    @GetMapping("/storages")
    public ResponseEntity<List<Map<String, Object>>> getStorages() {
        List<StorageSpace> spaces = storageService.getAllStorageSpaces();
        List<Map<String, Object>> result = new ArrayList<>();

        for (StorageSpace space : spaces) {
            Map<String, Object> spaceMap = new HashMap<>();
            spaceMap.put("name", space.getName());
            spaceMap.put("path", space.getPath());
            spaceMap.put("maxSize", space.getFormattedMaxSize());
            spaceMap.put("urlPrefix", space.getUrlPrefix());
            spaceMap.put("maxSizeBytes", space.getMaxSizeInBytes());
            spaceMap.put("allowAnonymous", space.isAllowAnonymous());

            StorageStats stats = storageService.getStorageStats(space.getName());
            if (stats != null) {
                spaceMap.put("usedSize", stats.getFormattedUsedSize());
                spaceMap.put("freeSize", stats.getFormattedFreeSize());
                spaceMap.put("imageCount", stats.getImageCount());
                spaceMap.put("directoryCount", stats.getDirectoryCount());
                spaceMap.put("usagePercentage", stats.getUsagePercentage());
            }

            result.add(spaceMap);
        }

        return ResponseEntity.ok(result);
    }

    /**
     * Create storage space
     */
    @PostMapping("/storages")
    public ResponseEntity<Map<String, Object>> createStorage(@RequestBody Map<String, Object> request) {
        String name = (String) request.get("name");
        String path = (String) request.get("path");
        String maxSize = (String) request.get("maxSize");
        String urlPrefix = (String) request.get("urlPrefix");
        Boolean allowAnonymous = request.get("allowAnonymous") != null ? (Boolean) request.get("allowAnonymous") : false;

        boolean success = storageService.createStorageSpace(name, path, maxSize, urlPrefix, allowAnonymous);

        Map<String, Object> response = new HashMap<>();
        response.put("success", success);

        if (!success) {
            response.put("error", ErrorMessages.getZh("failed_to_create_storage"));
            response.put("error_en", ErrorMessages.getEn("failed_to_create_storage"));
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Update storage space
     */
    @PutMapping("/storages/{name}")
    public ResponseEntity<Map<String, Object>> updateStorage(
            @PathVariable String name,
            @RequestBody Map<String, Object> request) {
        String path = (String) request.get("path");
        String maxSize = (String) request.get("maxSize");
        String urlPrefix = (String) request.get("urlPrefix");
        Boolean allowAnonymous = request.get("allowAnonymous") != null ? (Boolean) request.get("allowAnonymous") : false;

        boolean success = storageService.updateStorageSpace(name, path, maxSize, urlPrefix, allowAnonymous);

        Map<String, Object> response = new HashMap<>();
        response.put("success", success);

        if (!success) {
            response.put("error", ErrorMessages.getZh("failed_to_update_storage"));
            response.put("error_en", ErrorMessages.getEn("failed_to_update_storage"));
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Delete storage space
     */
    @DeleteMapping("/storages/{name}")
    public ResponseEntity<Map<String, Object>> deleteStorage(@PathVariable String name) {
        boolean success = storageService.deleteStorageSpace(name);

        Map<String, Object> response = new HashMap<>();
        response.put("success", success);

        if (!success) {
            response.put("error", ErrorMessages.getZh("failed_to_delete_storage"));
            response.put("error_en", ErrorMessages.getEn("failed_to_delete_storage"));
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Get users
     */
    @GetMapping("/users")
    public ResponseEntity<List<User>> getUsers() {
        List<User> users = userService.getAllUsers();

        // Remove passwords from response
        List<User> safeUsers = users.stream()
                .map(user -> {
                    User safeUser = new User();
                    safeUser.setUsername(user.getUsername());
                    safeUser.setRole(user.getRole());
                    safeUser.setStorageSpaces(user.getStorageSpaces());
                    return safeUser;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(safeUsers);
    }

    /**
     * Create user
     */
    @PostMapping("/users")
    public ResponseEntity<Map<String, Object>> createUser(@RequestBody Map<String, Object> request) {
        String username = (String) request.get("username");
        String password = (String) request.get("password");
        String roleStr = (String) request.get("role");
        @SuppressWarnings("unchecked")
        List<String> storageSpacesList = (List<String>) request.get("storageSpaces");

        Role role = Role.fromString(roleStr);
        String[] storageSpaces = storageSpacesList != null ? storageSpacesList.toArray(new String[0]) : new String[0];

        boolean success = userService.createUser(username, password, role, storageSpaces);

        Map<String, Object> response = new HashMap<>();
        response.put("success", success);

        if (!success) {
            response.put("error", ErrorMessages.getZh("failed_to_create_user"));
            response.put("error_en", ErrorMessages.getEn("failed_to_create_user"));
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Update user
     */
    @PutMapping("/users/{username}")
    public ResponseEntity<Map<String, Object>> updateUser(
            @PathVariable String username,
            @RequestBody Map<String, Object> request) {
        String password = (String) request.get("password");
        String roleStr = (String) request.get("role");
        @SuppressWarnings("unchecked")
        List<String> storageSpacesList = (List<String>) request.get("storageSpaces");

        Role role = Role.fromString(roleStr);
        String[] storageSpaces = storageSpacesList != null ? storageSpacesList.toArray(new String[0]) : new String[0];

        boolean success = userService.updateUser(username, password, role, storageSpaces);

        Map<String, Object> response = new HashMap<>();
        response.put("success", success);

        if (!success) {
            response.put("error", ErrorMessages.getZh("failed_to_update_user"));
            response.put("error_en", ErrorMessages.getEn("failed_to_update_user"));
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Delete user
     */
    @DeleteMapping("/users/{username}")
    public ResponseEntity<Map<String, Object>> deleteUser(@PathVariable String username) {
        boolean success = userService.deleteUser(username);

        Map<String, Object> response = new HashMap<>();
        response.put("success", success);

        if (!success) {
            response.put("error", ErrorMessages.getZh("failed_to_delete_user"));
            response.put("error_en", ErrorMessages.getEn("failed_to_delete_user"));
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Get system config
     */
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getConfig() {
        SystemConfig config = configService.getConfig();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("config", config);

        return ResponseEntity.ok(response);
    }

    /**
     * Update system config
     */
    @PutMapping("/config")
    public ResponseEntity<Map<String, Object>> updateConfig(@RequestBody SystemConfig config) {
        configService.saveConfig(config);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);

        return ResponseEntity.ok(response);
    }

    /**
     * Get API keys
     */
    @GetMapping("/apikeys")
    public ResponseEntity<List<SystemConfig.ApiKey>> getApiKeys() {
        SystemConfig config = configService.getConfig();
        return ResponseEntity.ok(config.getApiKeys() != null ? config.getApiKeys() : new ArrayList<>());
    }

    /**
     * Get theme
     */
    @GetMapping("/theme")
    public ResponseEntity<Map<String, Object>> getTheme() {
        SystemConfig config = configService.getConfig();
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("theme", config.getTheme() != null ? config.getTheme() : "light");
        return ResponseEntity.ok(response);
    }

    /**
     * Generate API key
     */
    @PostMapping("/apikeys")
    public ResponseEntity<Map<String, Object>> generateApiKey(@RequestBody Map<String, String> request) {
        String storageSpace = request.get("storageSpace");

        SystemConfig config = configService.getConfig();
        if (config.getApiKeys() == null) {
            config.setApiKeys(new ArrayList<>());
        }

        String token = generateApiKeyToken();
        SystemConfig.ApiKey apiKey = new SystemConfig.ApiKey();
        apiKey.setToken(token);
        apiKey.setStorageSpace(storageSpace);

        config.getApiKeys().add(apiKey);
        configService.saveConfig(config);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("token", token);

        return ResponseEntity.ok(response);
    }

    /**
     * Delete API key
     */
    @DeleteMapping("/apikeys/{index}")
    public ResponseEntity<Map<String, Object>> deleteApiKey(@PathVariable int index) {
        SystemConfig config = configService.getConfig();
        if (config.getApiKeys() != null && index >= 0 && index < config.getApiKeys().size()) {
            config.getApiKeys().remove(index);
            configService.saveConfig(config);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            return ResponseEntity.ok(response);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", ErrorMessages.getZh("invalid_api_key_index"));
        response.put("error_en", ErrorMessages.getEn("invalid_api_key_index"));

        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Upload image (admin)
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> adminUpload(@RequestParam("file") MultipartFile file,
                                                           @RequestParam("storageSpace") String storageSpace) {
        try {
            UploadResult result = imageService.uploadImage(file, storageSpace);

            Map<String, Object> response = new HashMap<>();
            response.put("success", result.isSuccess());
            response.put("message", result.getMessage());
            response.put("url", result.getUrl());
            response.put("path", result.getPath());

            if (result.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }
        } catch (IOException e) {
            logger.error("Admin upload failed", e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Upload image to custom path (admin)
     */
    @PostMapping("/upload-custom")
    public ResponseEntity<Map<String, Object>> adminUploadCustom(
            @RequestParam("file") MultipartFile file,
            @RequestParam("storageSpace") String storageSpace,
            @RequestParam(value = "targetPath", required = false) String targetPath) {
        try {
            UploadResult result = imageService.uploadImageToPath(file, storageSpace, targetPath);

            Map<String, Object> response = new HashMap<>();
            response.put("success", result.isSuccess());
            response.put("message", result.getMessage());
            response.put("url", result.getUrl());
            response.put("path", result.getPath());

            if (result.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }
        } catch (IOException e) {
            logger.error("Admin upload failed", e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Create directory
     */
    @PostMapping("/directory")
    public ResponseEntity<Map<String, Object>> createDirectory(@RequestBody Map<String, String> request) {
        String path = request.get("path");
        String storageSpace = request.get("storageSpace");

        boolean success = imageService.createDirectory(path, storageSpace);

        Map<String, Object> response = new HashMap<>();
        response.put("success", success);

        if (!success) {
            response.put("error", ErrorMessages.getZh("failed_to_create_directory"));
            response.put("error_en", ErrorMessages.getEn("failed_to_create_directory"));
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Rename directory
     */
    @PutMapping("/directory")
    public ResponseEntity<Map<String, Object>> renameDirectory(@RequestBody Map<String, String> request) {
        String oldPath = request.get("oldPath");
        String newPath = request.get("newPath");
        String storageSpace = request.get("storageSpace");

        boolean success = imageService.renameDirectory(oldPath, newPath, storageSpace);

        Map<String, Object> response = new HashMap<>();
        response.put("success", success);

        if (!success) {
            response.put("error", ErrorMessages.getZh("failed_to_rename_directory"));
            response.put("error_en", ErrorMessages.getEn("failed_to_rename_directory"));
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Rename image
     */
    @PostMapping("/rename-image")
    public ResponseEntity<Map<String, Object>> renameImage(@RequestBody Map<String, String> request) {
        String storageSpace = request.get("storageSpace");
        String oldPath = request.get("oldPath");
        String newPath = request.get("newPath");

        boolean success = imageService.renameImage(oldPath, newPath, storageSpace);

        Map<String, Object> response = new HashMap<>();
        response.put("success", success);

        if (!success) {
            response.put("error", ErrorMessages.getZh("failed_to_rename_directory"));
            response.put("error_en", ErrorMessages.getEn("failed_to_rename_directory"));
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Move image
     */
    @PostMapping("/move-image")
    public ResponseEntity<Map<String, Object>> moveImage(@RequestBody Map<String, String> request) {
        String storageSpace = request.get("storageSpace");
        String sourcePath = request.get("sourcePath");
        String targetPath = request.get("targetPath");

        boolean success = imageService.moveImage(sourcePath, targetPath, storageSpace);

        Map<String, Object> response = new HashMap<>();
        response.put("success", success);

        if (!success) {
            response.put("error", ErrorMessages.getZh("failed_to_rename_directory"));
            response.put("error_en", ErrorMessages.getEn("failed_to_rename_directory"));
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Delete directory
     */
    @DeleteMapping("/directory")
    public ResponseEntity<Map<String, Object>> deleteDirectory(@RequestBody Map<String, String> request) {
        String path = request.get("path");
        String storageSpace = request.get("storageSpace");

        boolean success = imageService.deleteDirectory(path, storageSpace);

        Map<String, Object> response = new HashMap<>();
        response.put("success", success);

        if (!success) {
            response.put("error", ErrorMessages.getZh("failed_to_delete_directory"));
            response.put("error_en", ErrorMessages.getEn("failed_to_delete_directory"));
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Get active sessions
     */
    @GetMapping("/sessions")
    public ResponseEntity<List<Map<String, Object>>> getSessions() {
        List<Map<String, Object>> sessions = authService.getActiveSessions();
        return ResponseEntity.ok(sessions);
    }

    /**
     * Generate API key token using secure random
     */
    private String generateApiKeyToken() {
        return com.wwh.simplepic.security.SecureTokenGenerator.generateApiKeyToken();
    }
}