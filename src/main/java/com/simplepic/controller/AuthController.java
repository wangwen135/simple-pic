package com.simplepic.controller;

import com.simplepic.model.LoginSession;
import com.simplepic.model.SystemConfig;
import com.simplepic.model.User;
import com.simplepic.service.AuthService;
import com.simplepic.service.ConfigService;
import com.simplepic.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
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

    /**
     * Login
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> request,
                                                     HttpServletResponse response) {
        String username = request.get("username");
        String password = request.get("password");
        boolean rememberMe = Boolean.parseBoolean(request.getOrDefault("rememberMe", "false"));

        String token = authService.login(username, password);

        if (token != null) {
            // Set cookie
            int maxAge = rememberMe ? 30 * 24 * 60 * 60 : 24 * 60 * 60; // 30 days or 1 day
            Cookie cookie = new Cookie("token", token);
            cookie.setHttpOnly(true);
            cookie.setPath("/");
            cookie.setMaxAge(maxAge);
            response.addCookie(cookie);

            User user = userService.getUser(username);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("token", token);
            result.put("user", user);

            logger.info("User {} logged in successfully", username);
            return ResponseEntity.ok(result);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("error", "Invalid username or password");
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

        // Clear cookie
        Cookie cookie = new Cookie("token", "");
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);

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
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("user", user);
            return ResponseEntity.ok(result);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("error", "Not authenticated");
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
        result.put("error", "Failed to switch storage space");
        return ResponseEntity.badRequest().body(result);
    }
}