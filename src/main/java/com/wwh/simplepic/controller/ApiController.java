package com.wwh.simplepic.controller;

import com.wwh.simplepic.model.StorageSpace;
import com.wwh.simplepic.model.SystemConfig;
import com.wwh.simplepic.model.UploadResult;
import com.wwh.simplepic.service.AuthService;
import com.wwh.simplepic.service.ConfigService;
import com.wwh.simplepic.service.ImageService;
import com.wwh.simplepic.util.ErrorMessages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * API controller for external access
 * API控制器
 */
@RestController
@RequestMapping("/api")
public class ApiController {

    private static final Logger logger = LoggerFactory.getLogger(ApiController.class);

    @Autowired
    private ImageService imageService;

    @Autowired
    private ConfigService configService;

    @Autowired
    private AuthService authService;

    /**
     * Upload image via API token
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> apiUpload(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "token", required = false) String tokenParam,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        try {
            // Get token from header or parameter
            String token = null;
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7);
            } else if (tokenParam != null) {
                token = tokenParam;
            }

            if (token == null || token.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("error", ErrorMessages.getZh("api_token_required"));
                response.put("error_en", ErrorMessages.getEn("api_token_required"));
                return ResponseEntity.status(401).body(response);
            }

            // Validate token
            String storageSpace = validateToken(token);
            if (storageSpace == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("error", ErrorMessages.getZh("invalid_api_token"));
                response.put("error_en", ErrorMessages.getEn("invalid_api_token"));
                return ResponseEntity.status(401).body(response);
            }

            // Upload image
            UploadResult result = imageService.uploadImage(file, storageSpace);

            if (result.isSuccess()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("url", result.getUrl());
                response.put("path", result.getPath());
                response.put("storageSpace", result.getStorageSpace());
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("error", result.getMessage());
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            logger.error("API upload failed", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", ErrorMessages.getZh("upload_failed") + ": " + e.getMessage());
            response.put("error_en", ErrorMessages.getEn("upload_failed") + ": " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Validate API token and return storage space
     */
    private String validateToken(String token) {
        SystemConfig config = configService.getConfig();
        if (config == null || config.getApiKeys() == null) {
            return null;
        }

        for (SystemConfig.ApiKey apiKey : config.getApiKeys()) {
            if (apiKey.getToken().equals(token)) {
                return apiKey.getStorageSpace();
            }
        }

        return null;
    }

    /**
     * Health check
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "ok");
        response.put("service", "Simple-Pic");
        return ResponseEntity.ok(response);
    }
}