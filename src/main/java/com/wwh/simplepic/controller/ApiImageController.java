package com.wwh.simplepic.controller;

import com.wwh.simplepic.model.UploadResult;
import com.wwh.simplepic.service.AuthService;
import com.wwh.simplepic.service.ConfigService;
import com.wwh.simplepic.service.ImageService;
import com.wwh.simplepic.util.ErrorMessages;
import com.wwh.simplepic.util.StorageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * API Image Controller - handles API endpoints under /api/image/*
 * API 图片控制器 - 处理 /api/image/* 下的 API 端点
 */
@RestController
@RequestMapping("/api/image")
public class ApiImageController {

    private static final Logger logger = LoggerFactory.getLogger(ApiImageController.class);

    @Autowired
    private ImageService imageService;

    @Autowired
    private AuthService authService;

    @Autowired
    private ConfigService configService;

    @Autowired
    private StorageUtils storageUtils;

    /**
     * Upload image via API
     * 通过 API 上传图片
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadImage(
            @RequestParam("file") MultipartFile file,
            @CookieValue(value = "token", required = false) String token,
            @RequestParam(value = "storageSpace", required = false) String storageSpace) {
        try {
            // Get current user
            com.wwh.simplepic.model.User user = authService.getCurrentUser(token);

            // Determine storage space
            if (storageSpace == null || storageSpace.isEmpty()) {
                if (user != null && user.getCurrentStorageSpace() != null) {
                    storageSpace = user.getCurrentStorageSpace();
                } else {
                    // Find first storage space that allows anonymous upload
                    storageSpace = storageUtils.findAnonymousUploadSpace();
                    if (storageSpace == null) {
                        storageSpace = "default";
                    }
                }
            }

            // Verify anonymous upload is allowed if not authenticated
            if (user == null) {
                com.wwh.simplepic.model.SystemConfig config = configService.getConfig();
                if (!config.isAnonymousUploadEnabled()) {
                    return ResponseEntity.status(403).body(createErrorResponse("anonymous_upload_not_enabled"));
                }

                // Check if the specific storage space allows anonymous upload
                if (!storageUtils.isAnonymousUploadAllowed(storageSpace)) {
                    return ResponseEntity.status(403).body(createErrorResponse("storage_no_anonymous"));
                }
            }

            UploadResult result = imageService.uploadImage(file, storageSpace);

            if (result.isSuccess()) {
                return ResponseEntity.ok(resultToMap(result));
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("error", result.getMessage());
                return ResponseEntity.badRequest().body(response);
            }
        } catch (IOException e) {
            logger.error("Upload failed", e);
            return ResponseEntity.status(500).body(createUploadFailedResponse(e.getMessage()));
        }
    }

    private Map<String, Object> createErrorResponse(String errorKey) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", ErrorMessages.getZh(errorKey));
        response.put("error_en", ErrorMessages.getEn(errorKey));
        return response;
    }

    private Map<String, Object> createUploadFailedResponse(String errorMessage) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", ErrorMessages.getZh("upload_failed") + ": " + errorMessage);
        response.put("error_en", ErrorMessages.getEn("upload_failed") + ": " + errorMessage);
        return response;
    }

    private Map<String, Object> resultToMap(UploadResult result) {
        Map<String, Object> map = new HashMap<>();
        map.put("success", result.isSuccess());
        map.put("message", result.getMessage());
        map.put("url", result.getUrl());
        map.put("thumbnailUrl", result.getThumbnailUrl());
        map.put("markdown", result.getMarkdown());
        map.put("html", result.getHtml());
        map.put("bbcode", result.getBbcode());
        map.put("directLink", result.getDirectLink());
        map.put("path", result.getPath());
        map.put("storageSpace", result.getStorageSpace());
        return map;
    }
}
