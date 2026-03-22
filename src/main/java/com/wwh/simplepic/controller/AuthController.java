package com.wwh.simplepic.controller;

import com.wwh.simplepic.model.LoginSession;
import com.wwh.simplepic.model.SystemConfig;
import com.wwh.simplepic.model.User;
import com.wwh.simplepic.service.AuthService;
import com.wwh.simplepic.service.ConfigService;
import com.wwh.simplepic.service.UserService;
import com.wwh.simplepic.util.ErrorMessages;
import com.wwh.simplepic.util.ResponseUtils;
import com.wwh.simplepic.util.SimplePicUtils;
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
            Map<String, Object> result = ResponseUtils.error("ip_locked");
            result.put("remainingMinutes", remainingMinutes);
            return ResponseEntity.status(429).body(result);
        }

        String token = authService.login(username, password, ipAddress, rememberMe);

        if (token != null) {
            // Set cookie with security attributes
            int maxAge = rememberMe ? 30 * 24 * 60 * 60 : 24 * 60 * 60; // 30 days or 1 day
            boolean isSecure = isProductionEnvironment();
            // Set SameSite cookie attribute for CSRF protection via header
            // Use Lax to allow cookies to be sent on same-site navigations
            String cookieHeader = String.format("token=%s; Path=/; HttpOnly; %s; Max-Age=%d; SameSite=Lax",
                    token,
                    isSecure ? "Secure" : "",
                    maxAge);
            response.addHeader("Set-Cookie", cookieHeader);

            User user = userService.getUser(username);
            Map<String, Object> result = ResponseUtils.success();
            result.put("token", token);
            result.put("user", user);

            logger.info("User {} logged in successfully from IP: {}", username, ipAddress);
            return ResponseEntity.ok(result);
        }

        return ResponseEntity.status(401).body(ResponseUtils.error("invalid_credentials"));
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
        String cookieHeader = String.format("token=; Path=/; HttpOnly; %s; Max-Age=0; SameSite=Lax",
                isSecure ? "Secure" : "");
        response.addHeader("Set-Cookie", cookieHeader);

        return ResponseEntity.ok(ResponseUtils.success());
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

            return ResponseEntity.ok(ResponseUtils.success("user", safeUserInfo));
        }

        return ResponseEntity.status(401).body(ResponseUtils.error("not_authenticated"));
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
            return ResponseEntity.ok(ResponseUtils.success("storageSpace", storageSpace));
        }

        return ResponseEntity.badRequest().body(ResponseUtils.error("failed_to_switch_storage"));
    }

    /**
     * Get auth config (for anonymous upload)
     */
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getAuthConfig() {
        SystemConfig config = configService.getConfig();

        // Get storage spaces that allow anonymous upload
        List<Map<String, String>> availableSpaces = new ArrayList<>();
        if (config.getStorageSpaces() != null) {
            for (SystemConfig.StorageSpace space : config.getStorageSpaces()) {
                if (space.isAllowAnonymous()) {
                    Map<String, String> spaceInfo = new HashMap<>();
                    spaceInfo.put("name", space.getName());
                    spaceInfo.put("domain", space.getUrlPrefix());
                    availableSpaces.add(spaceInfo);
                }
            }
        }

        Map<String, Object> result = ResponseUtils.success();
        result.put("anonymousUploadEnabled", config.isAnonymousUploadEnabled());
        result.put("systemName", config.getName());
        result.put("systemDescription", config.getDescription());
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
            return ResponseEntity.status(401).body(ResponseUtils.error("not_authenticated"));
        }

        String currentPassword = request.get("currentPassword");
        String newPassword = request.get("newPassword");

        if (currentPassword == null || newPassword == null) {
            return ResponseEntity.badRequest().body(ResponseUtils.error("invalid_file_format"));
        }

        // Verify current password
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            return ResponseEntity.badRequest().body(ResponseUtils.error("invalid_current_password"));
        }

        // Change password
        if (userService.changePassword(user.getUsername(), newPassword)) {
            return ResponseEntity.ok(ResponseUtils.success());
        }

        return ResponseEntity.badRequest().body(ResponseUtils.error("failed_to_update_user"));
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