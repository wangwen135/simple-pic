package com.simplepic.controller;

import com.simplepic.model.LoginSession;
import com.simplepic.model.SystemConfig;
import com.simplepic.model.User;
import com.simplepic.service.AuthService;
import com.simplepic.service.ConfigService;
import com.simplepic.service.UserService;
import com.simplepic.util.ErrorMessages;
import com.simplepic.util.SimplePicUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Authentication controller
 * 认证控制器
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private AuthService authService;

    @Autowired
    private UserService userService;

    @Autowired
    private ConfigService configService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Login
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> request,
                                                     HttpServletResponse response,
                                                     HttpServletRequest httpRequest) {
        String username = request.get("username");
        String password = request.get("password");
        boolean rememberMe = Boolean.parseBoolean(request.getOrDefault("rememberMe", "false"));

        // Get client IP address
        String ipAddress = SimplePicUtils.getClientIpAddress(httpRequest);

        // Check if IP is locked
        if (authService.isIPLocked(ipAddress)) {
            long remainingMinutes = authService.getRemainingLockoutMinutes(ipAddress);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("error", ErrorMessages.getZh("ip_locked"));
            result.put("error_en", ErrorMessages.getEn("ip_locked"));
            result.put("remainingMinutes", remainingMinutes);
            return ResponseEntity.status(429).body(result);
        }

        String token = authService.login(username, password, ipAddress, rememberMe);

        if (token != null) {
            // Set cookie with security attributes
            int maxAge = rememberMe ? 30 * 24 * 60 * 60 : 24 * 60 * 60; // 30 days or 1 day
            boolean isSecure = isProductionEnvironment();
            // Set SameSite cookie attribute for CSRF protection via header
            String cookieHeader = String.format("token=%s; Path=/; HttpOnly; %s; Max-Age=%d; SameSite=Strict",
                    token,
                    isSecure ? "Secure" : "",
                    maxAge);
            response.addHeader("Set-Cookie", cookieHeader);

            User user = userService.getUser(username);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("token", token);
            result.put("user", user);

            logger.info("User {} logged in successfully from IP: {}", username, ipAddress);
            return ResponseEntity.ok(result);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("error", ErrorMessages.getZh("invalid_credentials"));
        result.put("error_en", ErrorMessages.getEn("invalid_credentials"));
        return ResponseEntity.status(401).body(result);
    }

    /**
     * Logout
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(@CookieValue(value = "token", required = false) String token,
                                                      HttpServletResponse response) {
        if (token != null) {
            authService.logout(token);
        }

        // Clear cookie with same security attributes via Set-Cookie header
        boolean isSecure = isProductionEnvironment();
        String cookieHeader = String.format("token=; Path=/; HttpOnly; %s; Max-Age=0; SameSite=Strict",
                isSecure ? "Secure" : "");
        response.addHeader("Set-Cookie", cookieHeader);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        return ResponseEntity.ok(result);
    }

    /**
     * Get current user info
     */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(@CookieValue(value = "token", required = false) String token) {
        User user = authService.getCurrentUser(token);

        if (user != null) {
            // Create safe user info without password
            Map<String, Object> safeUserInfo = new HashMap<>();
            safeUserInfo.put("username", user.getUsername());
            safeUserInfo.put("role", user.getRole() != null ? user.getRole().toString() : null);
            safeUserInfo.put("storageSpaces", user.getStorageSpaces());
            safeUserInfo.put("currentStorageSpace", user.getCurrentStorageSpace());

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("user", safeUserInfo);
            return ResponseEntity.ok(result);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("error", ErrorMessages.getZh("not_authenticated"));
        result.put("error_en", ErrorMessages.getEn("not_authenticated"));
        return ResponseEntity.status(401).body(result);
    }

    /**
     * Change current storage space
     */
    @PostMapping("/switch-space")
    public ResponseEntity<Map<String, Object>> switchStorageSpace(
            @CookieValue(value = "token", required = false) String token,
            @RequestBody Map<String, String> request) {
        String storageSpace = request.get("storageSpace");

        if (authService.switchStorageSpace(token, storageSpace)) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("storageSpace", storageSpace);
            return ResponseEntity.ok(result);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("error", ErrorMessages.getZh("failed_to_switch_storage"));
        result.put("error_en", ErrorMessages.getEn("failed_to_switch_storage"));
        return ResponseEntity.badRequest().body(result);
    }

    /**
     * Get auth config (for anonymous upload)
     */
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getAuthConfig() {
        SystemConfig config = configService.getConfig();
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("anonymousUploadEnabled", config.isAnonymousUploadEnabled());
        result.put("systemName", config.getName());
        result.put("systemDescription", config.getDescription());

        // Get storage spaces that allow anonymous upload
        List<Map<String, String>> availableSpaces = new ArrayList<>();
        if (config.getStorageSpaces() != null) {
            for (SystemConfig.StorageSpace space : config.getStorageSpaces()) {
                if (space.isAllowAnonymous()) {
                    Map<String, String> spaceInfo = new HashMap<>();
                    spaceInfo.put("name", space.getName());
                    spaceInfo.put("domain", space.getDomain());
                    availableSpaces.add(spaceInfo);
                }
            }
        }
        result.put("availableStorageSpaces", availableSpaces);

        return ResponseEntity.ok(result);
    }

    /**
     * Change password
     */
    @PostMapping("/change-password")
    public ResponseEntity<Map<String, Object>> changePassword(
            @CookieValue(value = "token", required = false) String token,
            @RequestBody Map<String, String> request) {
        User user = authService.getCurrentUser(token);

        if (user == null) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("error", ErrorMessages.getZh("not_authenticated"));
            result.put("error_en", ErrorMessages.getEn("not_authenticated"));
            return ResponseEntity.status(401).body(result);
        }

        String currentPassword = request.get("currentPassword");
        String newPassword = request.get("newPassword");

        if (currentPassword == null || newPassword == null) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("error", ErrorMessages.getZh("invalid_file_format"));
            result.put("error_en", ErrorMessages.getEn("invalid_file_format"));
            return ResponseEntity.badRequest().body(result);
        }

        // Verify current password
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("error", ErrorMessages.getZh("invalid_current_password"));
            result.put("error_en", ErrorMessages.getEn("invalid_current_password"));
            return ResponseEntity.badRequest().body(result);
        }

        // Change password
        if (userService.changePassword(user.getUsername(), newPassword)) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            return ResponseEntity.ok(result);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("error", ErrorMessages.getZh("failed_to_update_user"));
        result.put("error_en", ErrorMessages.getEn("failed_to_update_user"));
        return ResponseEntity.badRequest().body(result);
    }

    /**
     * Check if running in production environment
     */
    private boolean isProductionEnvironment() {
        // Check for common production indicators
        String env = System.getProperty("spring.profiles.active");
        return "prod".equalsIgnoreCase(env) || "production".equalsIgnoreCase(env);
    }
}