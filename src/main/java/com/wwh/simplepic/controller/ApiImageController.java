package com.wwh.simplepic.controller;

import com.wwh.simplepic.model.UploadResult;
import com.wwh.simplepic.service.AuthService;
import com.wwh.simplepic.service.ConfigService;
import com.wwh.simplepic.service.ImageService;
import com.wwh.simplepic.util.ResponseUtils;
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
                if (config == null || !config.isAnonymousUploadEnabled()) {
                    return ResponseEntity.status(403).body(ResponseUtils.error("anonymous_upload_not_enabled"));
                }

                // Check if the specific storage space allows anonymous upload
                if (!storageUtils.isAnonymousUploadAllowed(storageSpace)) {
                    return ResponseEntity.status(403).body(ResponseUtils.error("storage_no_anonymous"));
                }
            }

            UploadResult result = imageService.uploadImage(file, storageSpace);

            if (result.isSuccess()) {
                return ResponseEntity.ok(resultToMap(result));
            } else {
                Map<String, Object> response = ResponseUtils.error(result.getMessage(), result.getMessage());
                return ResponseEntity.badRequest().body(response);
            }
        } catch (IOException e) {
            logger.error("Upload failed", e);
            return ResponseEntity.status(500).body(ResponseUtils.errorWithDetail("upload_failed", e.getMessage()));
        }
    }

    /**
     * 将 UploadResult 转换为 Map
     */
    private Map<String, Object> resultToMap(UploadResult result) {
        Map<String, Object> response = ResponseUtils.success();
        response.put("message", result.getMessage());
        response.put("url", result.getUrl());
        response.put("markdown", result.getMarkdown());
        response.put("html", result.getHtml());
        response.put("bbcode", result.getBbcode());
        response.put("directLink", result.getDirectLink());
        response.put("path", result.getPath());
        response.put("storageSpace", result.getStorageSpace());
        return response;
    }
}
