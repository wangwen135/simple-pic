package com.simplepic.controller;

import com.simplepic.model.ImageInfo;
import com.simplepic.model.UploadResult;
import com.simplepic.model.User;
import com.simplepic.service.AuthService;
import com.simplepic.service.ImageService;
import com.simplepic.service.ThumbnailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Image controller
 * 图片控制器
 */
@RestController
@RequestMapping("/api/image")
public class ImageController {

    private static final Logger logger = LoggerFactory.getLogger(ImageController.class);

    @Autowired
    private ImageService imageService;

    @Autowired
    private AuthService authService;

    @Autowired
    private ThumbnailService thumbnailService;

    /**
     * Upload image
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadImage(
            @RequestParam("file") MultipartFile file,
            @CookieValue(value = "token", required = false) String token,
            @RequestParam(value = "storageSpace", required = false) String storageSpace) {
        try {
            // Get storage space from session or parameter
            if (storageSpace == null || storageSpace.isEmpty()) {
                com.simplepic.model.User user = authService.getCurrentUser(token);
                if (user != null && user.getCurrentStorageSpace() != null) {
                    storageSpace = user.getCurrentStorageSpace();
                } else {
                    storageSpace = "default";
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
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", "Upload failed: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * List images
     */
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> listImages(
            @CookieValue(value = "token", required = false) String token,
            @RequestParam(value = "path", required = false) String path,
            @RequestParam(value = "storageSpace", required = false) String storageSpace,
            @RequestParam(value = "recursive", defaultValue = "false") boolean recursive) {
        // Get storage space from session or parameter
        if (storageSpace == null || storageSpace.isEmpty()) {
            com.simplepic.model.User user = authService.getCurrentUser(token);
            if (user != null && user.getCurrentStorageSpace() != null) {
                storageSpace = user.getCurrentStorageSpace();
            } else {
                storageSpace = "default";
            }
        }

        List<ImageInfo> images = imageService.listImages(path, storageSpace, recursive);
        List<String> directories = imageService.listDirectories(path, storageSpace);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("images", images);
        response.put("directories", directories);
        response.put("path", path);
        response.put("storageSpace", storageSpace);

        return ResponseEntity.ok(response);
    }

    /**
     * Get image
     */
    @GetMapping("/{storageSpace}/**")
    public ResponseEntity<Resource> getImage(
            @PathVariable("storageSpace") String storageSpace,
            HttpServletRequest request) {
        String path = request.getRequestURI();
        path = path.substring("/api/image/".length() + storageSpace.length() + 1);

        File imageFile = imageService.getImageFile(path, storageSpace);

        if (imageFile == null || !imageFile.exists()) {
            return ResponseEntity.notFound().build();
        }

        String contentType = getContentType(imageFile.getName());
        Resource resource = new FileSystemResource(imageFile);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000")
                .body(resource);
    }

    /**
     * Get thumbnail
     */
    @GetMapping("/thumb/{storageSpace}/**")
    public ResponseEntity<Resource> getThumbnail(
            @PathVariable("storageSpace") String storageSpace,
            HttpServletRequest request) {
        String path = request.getRequestURI();
        path = path.substring("/api/image/thumb/".length() + storageSpace.length() + 1);

        File thumbnailFile = thumbnailService.getThumbnail(path, storageSpace);

        if (thumbnailFile == null || !thumbnailFile.exists()) {
            // Return original image if thumbnail doesn't exist
            return getImage(storageSpace, request);
        }

        String contentType = getContentType(thumbnailFile.getName());
        Resource resource = new FileSystemResource(thumbnailFile);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000")
                .body(resource);
    }

    /**
     * Delete image
     */
    @DeleteMapping("/{storageSpace}/**")
    public ResponseEntity<Map<String, Object>> deleteImage(
            @PathVariable("storageSpace") String storageSpace,
            HttpServletRequest request) {
        String path = request.getRequestURI();
        path = path.substring("/api/image/".length() + storageSpace.length() + 1);

        boolean success = imageService.deleteImage(path, storageSpace);

        Map<String, Object> response = new HashMap<>();
        response.put("success", success);

        if (success) {
            return ResponseEntity.ok(response);
        } else {
            response.put("error", "Failed to delete image");
            return ResponseEntity.badRequest().body(response);
        }
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

    private String getContentType(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (lower.endsWith(".png")) {
            return "image/png";
        } else if (lower.endsWith(".gif")) {
            return "image/gif";
        } else if (lower.endsWith(".webp")) {
            return "image/webp";
        } else if (lower.endsWith(".svg")) {
            return "image/svg+xml";
        }
        return "application/octet-stream";
    }
}