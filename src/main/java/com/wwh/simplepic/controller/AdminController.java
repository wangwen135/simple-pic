package com.wwh.simplepic.controller;

import com.wwh.simplepic.model.*;
import com.wwh.simplepic.service.*;
import com.wwh.simplepic.util.ErrorMessages;
import com.wwh.simplepic.util.ResponseUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
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
    private WatermarkService watermarkService;

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
            spaceMap.put("watermark", space.getWatermark());

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

        // 参数校验
        if (name == null || name.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ResponseUtils.error("name_required"));
        }
        if (path == null || path.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ResponseUtils.error("path_required"));
        }

        // 提取水印配置
        @SuppressWarnings("unchecked")
        Map<String, Object> watermarkData = (Map<String, Object>) request.get("watermark");
        WatermarkConfig watermarkConfig = parseWatermarkConfig(watermarkData);

        boolean success = storageService.createStorageSpace(name, path, maxSize, urlPrefix, allowAnonymous, watermarkConfig);

        if (success) {
            return ResponseEntity.ok(ResponseUtils.success());
        } else {
            return ResponseEntity.ok(ResponseUtils.error("failed_to_create_storage"));
        }
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

        // 参数校验
        if (path == null || path.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ResponseUtils.error("path_required"));
        }

        // 提取水印配置
        @SuppressWarnings("unchecked")
        Map<String, Object> watermarkData = (Map<String, Object>) request.get("watermark");
        WatermarkConfig watermarkConfig = parseWatermarkConfig(watermarkData);

        boolean success = storageService.updateStorageSpace(name, path, maxSize, urlPrefix, allowAnonymous, watermarkConfig);

        if (success) {
            // 水印设置变更时清除缓存
            watermarkService.clearWatermarkCache(name);
        }

        if (success) {
            return ResponseEntity.ok(ResponseUtils.success());
        } else {
            return ResponseEntity.ok(ResponseUtils.error("failed_to_update_storage"));
        }
    }

    /**
     * Delete storage space
     */
    @DeleteMapping("/storages/{name}")
    public ResponseEntity<Map<String, Object>> deleteStorage(@PathVariable String name) {
        boolean success = storageService.deleteStorageSpace(name);

        if (success) {
            return ResponseEntity.ok(ResponseUtils.success());
        } else {
            return ResponseEntity.ok(ResponseUtils.error("failed_to_delete_storage"));
        }
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

        // 参数校验
        if (username == null || username.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ResponseUtils.error("username_required"));
        }
        if (password == null || password.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ResponseUtils.error("password_required"));
        }
        if (roleStr == null || roleStr.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ResponseUtils.error("role_required"));
        }

        Role role = Role.fromString(roleStr);
        String[] storageSpaces = storageSpacesList != null ? storageSpacesList.toArray(new String[0]) : new String[0];

        boolean success = userService.createUser(username, password, role, storageSpaces);

        if (success) {
            return ResponseEntity.ok(ResponseUtils.success());
        } else {
            return ResponseEntity.ok(ResponseUtils.error("failed_to_create_user"));
        }
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

        if (success) {
            return ResponseEntity.ok(ResponseUtils.success());
        } else {
            return ResponseEntity.ok(ResponseUtils.error("failed_to_update_user"));
        }
    }

    /**
     * Delete user
     */
    @DeleteMapping("/users/{username}")
    public ResponseEntity<Map<String, Object>> deleteUser(@PathVariable String username) {
        boolean success = userService.deleteUser(username);

        if (success) {
            return ResponseEntity.ok(ResponseUtils.success());
        } else {
            return ResponseEntity.ok(ResponseUtils.error("failed_to_delete_user"));
        }
    }

    /**
     * Get system config
     */
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getConfig() {
        SystemConfig config = configService.getConfig();
        return ResponseEntity.ok(ResponseUtils.success("config", config));
    }

    /**
     * Update system config
     * Only updates system settings, preserves users, storage spaces, and API keys
     */
    @PutMapping("/config")
    public ResponseEntity<Map<String, Object>> updateConfig(@RequestBody SystemConfig config) {
        configService.updateSystemSettings(config);
        return ResponseEntity.ok(ResponseUtils.success());
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
     * Generate API key
     */
    @PostMapping("/apikeys")
    public ResponseEntity<Map<String, Object>> generateApiKey(@RequestBody Map<String, String> request) {
        String storageSpace = request.get("storageSpace");
        String remark = request.get("remark");

        // 参数校验
        if (storageSpace == null || storageSpace.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ResponseUtils.error("storage_space_required"));
        }

        SystemConfig config = configService.getConfig();
        if (config.getApiKeys() == null) {
            config.setApiKeys(new ArrayList<>());
        }

        String token = generateApiKeyToken();
        SystemConfig.ApiKey apiKey = new SystemConfig.ApiKey();
        apiKey.setToken(token);
        apiKey.setStorageSpace(storageSpace);
        apiKey.setRemark(remark);

        config.getApiKeys().add(apiKey);
        configService.saveConfig(config);

        return ResponseEntity.ok(ResponseUtils.success("token", token));
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
            return ResponseEntity.ok(ResponseUtils.success());
        }

        return ResponseEntity.badRequest().body(ResponseUtils.error("invalid_api_key_index"));
    }

    /**
     * Upload image (admin)
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> adminUpload(@RequestParam("file") MultipartFile file,
                                                           @RequestParam("storageSpace") String storageSpace) {
        try {
            UploadResult result = imageService.uploadImage(file, storageSpace);

            Map<String, Object> data = new HashMap<>();
            data.put("success", result.isSuccess());
            data.put("message", result.getMessage());
            data.put("url", result.getUrl());
            data.put("path", result.getPath());

            if (result.isSuccess()) {
                return ResponseEntity.ok(data);
            } else {
                return ResponseEntity.badRequest().body(data);
            }
        } catch (IOException e) {
            logger.error("Admin upload failed", e);
            return ResponseEntity.status(500).body(ResponseUtils.errorMessage(e.getMessage()));
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

            Map<String, Object> data = new HashMap<>();
            data.put("success", result.isSuccess());
            data.put("message", result.getMessage());
            data.put("url", result.getUrl());
            data.put("path", result.getPath());

            if (result.isSuccess()) {
                return ResponseEntity.ok(data);
            } else {
                return ResponseEntity.badRequest().body(data);
            }
        } catch (IOException e) {
            logger.error("Admin upload failed", e);
            return ResponseEntity.status(500).body(ResponseUtils.errorMessage(e.getMessage()));
        }
    }

    /**
     * Create directory
     */
    @PostMapping("/directory")
    public ResponseEntity<Map<String, Object>> createDirectory(@RequestBody Map<String, String> request) {
        String path = request.get("path");
        String storageSpace = request.get("storageSpace");

        // 参数校验
        if (path == null || path.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ResponseUtils.error("path_required"));
        }
        if (storageSpace == null || storageSpace.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ResponseUtils.error("storage_space_required"));
        }

        boolean success = imageService.createDirectory(path, storageSpace);

        if (success) {
            return ResponseEntity.ok(ResponseUtils.success());
        } else {
            return ResponseEntity.ok(ResponseUtils.error("failed_to_create_directory"));
        }
    }

    /**
     * Rename directory
     */
    @PutMapping("/directory")
    public ResponseEntity<Map<String, Object>> renameDirectory(@RequestBody Map<String, String> request) {
        String oldPath = request.get("oldPath");
        String newPath = request.get("newPath");
        String storageSpace = request.get("storageSpace");

        // 参数校验
        if (oldPath == null || oldPath.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ResponseUtils.error("old_path_required"));
        }
        if (newPath == null || newPath.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ResponseUtils.error("new_path_required"));
        }
        if (storageSpace == null || storageSpace.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ResponseUtils.error("storage_space_required"));
        }

        boolean success = imageService.renameDirectory(oldPath, newPath, storageSpace);

        if (success) {
            return ResponseEntity.ok(ResponseUtils.success());
        } else {
            return ResponseEntity.ok(ResponseUtils.error("failed_to_rename_directory"));
        }
    }

    /**
     * Rename image
     */
    @PostMapping("/rename-image")
    public ResponseEntity<Map<String, Object>> renameImage(@RequestBody Map<String, String> request) {
        String storageSpace = request.get("storageSpace");
        String oldPath = request.get("oldPath");
        String newPath = request.get("newPath");

        // 参数校验
        if (storageSpace == null || storageSpace.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ResponseUtils.error("storage_space_required"));
        }
        if (oldPath == null || oldPath.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ResponseUtils.error("old_path_required"));
        }
        if (newPath == null || newPath.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ResponseUtils.error("new_path_required"));
        }

        boolean success = imageService.renameImage(oldPath, newPath, storageSpace);

        if (success) {
            return ResponseEntity.ok(ResponseUtils.success());
        } else {
            return ResponseEntity.ok(ResponseUtils.error("failed_to_rename_directory"));
        }
    }

    /**
     * Move image
     */
    @PostMapping("/move-image")
    public ResponseEntity<Map<String, Object>> moveImage(@RequestBody Map<String, String> request) {
        String storageSpace = request.get("storageSpace");
        String sourcePath = request.get("sourcePath");
        String targetPath = request.get("targetPath");

        // 参数校验
        if (storageSpace == null || storageSpace.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ResponseUtils.error("storage_space_required"));
        }
        if (sourcePath == null || sourcePath.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ResponseUtils.error("source_path_required"));
        }
        if (targetPath == null || targetPath.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ResponseUtils.error("target_path_required"));
        }

        boolean success = imageService.moveImage(sourcePath, targetPath, storageSpace);

        if (success) {
            return ResponseEntity.ok(ResponseUtils.success());
        } else {
            return ResponseEntity.ok(ResponseUtils.error("failed_to_rename_directory"));
        }
    }

    /**
     * Delete directory
     */
    @DeleteMapping("/directory")
    public ResponseEntity<Map<String, Object>> deleteDirectory(@RequestBody Map<String, String> request) {
        String path = request.get("path");
        String storageSpace = request.get("storageSpace");

        // 参数校验
        if (path == null || path.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ResponseUtils.error("path_required"));
        }
        if (storageSpace == null || storageSpace.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ResponseUtils.error("storage_space_required"));
        }

        boolean success = imageService.deleteDirectory(path, storageSpace);

        if (success) {
            return ResponseEntity.ok(ResponseUtils.success());
        } else {
            return ResponseEntity.ok(ResponseUtils.error("failed_to_delete_directory"));
        }
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

    /**
     * 从请求体解析水印配置
     */
    private WatermarkConfig parseWatermarkConfig(Map<String, Object> data) {
        if (data == null) {
            return null;
        }
        WatermarkConfig wm = new WatermarkConfig();
        wm.setEnabled(data.get("enabled") != null && Boolean.TRUE.equals(data.get("enabled")));
        wm.setType((String) data.getOrDefault("type", "text"));
        wm.setContent((String) data.getOrDefault("content", ""));
        wm.setPosition((String) data.getOrDefault("position", "bottom-right"));
        wm.setOpacity(data.get("opacity") != null ? ((Number) data.get("opacity")).doubleValue() : 0.5);
        return wm;
    }

    /**
     * Upload custom hotlink protection image
     * 上传自定义防盗链提示图
     */
    @PostMapping("/hotlink-image")
    public ResponseEntity<Map<String, Object>> uploadHotlinkImage(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(ResponseUtils.error("file_is_empty"));
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            return ResponseEntity.badRequest().body(ResponseUtils.error("invalid_filename"));
        }

        String extension = "";
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex > 0) {
            extension = originalFilename.substring(dotIndex + 1).toLowerCase();
        }

        List<String> allowedExts = Arrays.asList("jpg", "jpeg", "png", "gif", "webp");
        if (!allowedExts.contains(extension)) {
            return ResponseEntity.badRequest().body(ResponseUtils.error("invalid_file_format"));
        }

        try {
            // Create hotlink directory
            File hotlinkDir = new File("hotlink");
            if (!hotlinkDir.exists()) {
                hotlinkDir.mkdirs();
            }

            // Save with fixed filename
            String filename = "hotlink-image." + extension;
            File targetFile = new File(hotlinkDir, filename);

            // Remove old file if different extension
            if (targetFile.exists()) {
                targetFile.delete();
            } else {
                // Clean up any previous hotlink images with different extensions
                for (String ext : allowedExts) {
                    File old = new File(hotlinkDir, "hotlink-image." + ext);
                    if (old.exists() && !ext.equals(extension)) {
                        old.delete();
                    }
                }
            }

            try (InputStream is = file.getInputStream()) {
                Files.copy(is, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }

            // Update config with the new image path
            SystemConfig config = configService.getConfig();
            config.setHotlinkImagePath(targetFile.getAbsolutePath());
            configService.saveConfig(config);

            return ResponseEntity.ok(ResponseUtils.success("image_path", targetFile.getAbsolutePath()));
        } catch (IOException e) {
            logger.error("Failed to upload hotlink image", e);
            return ResponseEntity.internalServerError().body(ResponseUtils.error("upload_failed"));
        }
    }

    /**
     * Preview hotlink protection image
     * 预览防盗链提示图
     */
    @GetMapping("/hotlink-image/preview")
    public ResponseEntity<Resource> previewHotlinkImage() {
        SystemConfig config = configService.getConfig();
        String imagePath = config.getHotlinkImagePath();
        if (imagePath == null || imagePath.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        File imageFile = new File(imagePath);
        if (!imageFile.exists()) {
            return ResponseEntity.notFound().build();
        }

        String name = imageFile.getName().toLowerCase();
        String contentType;
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) {
            contentType = "image/jpeg";
        } else if (name.endsWith(".png")) {
            contentType = "image/png";
        } else if (name.endsWith(".gif")) {
            contentType = "image/gif";
        } else if (name.endsWith(".webp")) {
            contentType = "image/webp";
        } else {
            contentType = "image/png";
        }

        return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.parseMediaType(contentType))
                .header(org.springframework.http.HttpHeaders.CACHE_CONTROL, "no-cache")
                .body(new org.springframework.core.io.FileSystemResource(imageFile));
    }
}