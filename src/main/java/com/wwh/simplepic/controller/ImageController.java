package com.wwh.simplepic.controller;

import com.wwh.simplepic.model.ImageInfo;
import com.wwh.simplepic.model.UploadResult;
import com.wwh.simplepic.model.User;
import com.wwh.simplepic.model.StorageSpace;
import com.wwh.simplepic.model.WatermarkConfig;
import com.wwh.simplepic.service.AuthService;
import com.wwh.simplepic.service.ConfigService;
import com.wwh.simplepic.service.ImageService;
import com.wwh.simplepic.service.StorageService;
import com.wwh.simplepic.service.WatermarkService;
import com.wwh.simplepic.util.ErrorMessages;
import com.wwh.simplepic.util.ResponseUtils;
import com.wwh.simplepic.util.SimplePicUtils;
import com.wwh.simplepic.util.StorageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.AntPathMatcher;
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
 * 图片控制器
 */
@RestController
@RequestMapping("/image")
public class ImageController {

    private static final Logger logger = LoggerFactory.getLogger(ImageController.class);

    @Autowired
    private ImageService imageService;

    @Autowired
    private AuthService authService;

    @Autowired
    private ConfigService configService;

    @Autowired
    private StorageService storageService;

    @Autowired
    private WatermarkService watermarkService;

    @Autowired
    private StorageUtils storageUtils;

    /**
     * 上传图片
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
                return ResponseEntity.badRequest().body(ResponseUtils.errorMessage(result.getMessage()));
            }
        } catch (IOException e) {
            logger.error("Upload failed", e);
            return ResponseEntity.status(500).body(ResponseUtils.errorWithDetail("upload_failed", e.getMessage()));
        }
    }

    /**
     * List images with pagination, search, filter and sort
     */
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> listImages(
            @CookieValue(value = "token", required = false) String token,
            @RequestParam(value = "path", required = false) String path,
            @RequestParam(value = "storageSpace", required = false) String storageSpace,
            @RequestParam(value = "recursive", defaultValue = "false") boolean recursive,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "pageSize", defaultValue = "50") int pageSize,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "sortBy", defaultValue = "date") String sortBy,
            @RequestParam(value = "sortOrder", defaultValue = "desc") String sortOrder,
            @RequestParam(value = "fileType", required = false) String fileType) {
        // Get storage space from session or parameter
        if (storageSpace == null || storageSpace.isEmpty()) {
            com.wwh.simplepic.model.User user = authService.getCurrentUser(token);
            if (user != null && user.getCurrentStorageSpace() != null) {
                storageSpace = user.getCurrentStorageSpace();
            } else {
                storageSpace = "default";
            }
        }

        List<ImageInfo> images = imageService.listImages(path, storageSpace, recursive);
        List<String> directories = imageService.listDirectories(path, storageSpace);

        // Apply search filter
        if (search != null && !search.isEmpty()) {
            String searchLower = search.toLowerCase();
            images = images.stream()
                    .filter(img -> img.getName().toLowerCase().contains(searchLower))
                    .collect(java.util.stream.Collectors.toList());
        }

        // Apply file type filter
        if (fileType != null && !fileType.equals("all")) {
            images = images.stream()
                    .filter(img -> {
                        String ext = img.getName().substring(img.getName().lastIndexOf('.') + 1).toLowerCase();
                        switch (fileType) {
                            case "image":
                                return java.util.Arrays.asList("jpg", "jpeg", "png", "gif", "webp", "svg").contains(ext);
                            default:
                                return true;
                        }
                    })
                    .collect(java.util.stream.Collectors.toList());
        }

        // Apply sorting
        final String finalSortBy = sortBy;
        final String finalSortOrder = sortOrder;
        images.sort((a, b) -> {
            int comparison = 0;
            switch (finalSortBy) {
                case "name":
                    comparison = a.getName().compareToIgnoreCase(b.getName());
                    break;
                case "size":
                    comparison = Long.compare(a.getSize(), b.getSize());
                    break;
                case "date":
                default:
                    comparison = Long.compare(a.getLastModified(), b.getLastModified());
                    break;
            }
            return "desc".equalsIgnoreCase(finalSortOrder) ? -comparison : comparison;
        });

        // Pagination
        int total = images.size();
        int totalPages = (int) Math.ceil((double) total / pageSize);
        int startIndex = (page - 1) * pageSize;
        int endIndex = Math.min(startIndex + pageSize, total);

        List<ImageInfo> pagedImages = startIndex < total ?
                images.subList(startIndex, endIndex) : new java.util.ArrayList<>();

        Map<String, Object> pagination = new HashMap<>();
        pagination.put("page", page);
        pagination.put("pageSize", pageSize);
        pagination.put("total", total);
        pagination.put("totalPages", totalPages);

        Map<String, Object> response = ResponseUtils.success();
        response.put("images", pagedImages);
        response.put("directories", directories);
        response.put("path", path);
        response.put("storageSpace", storageSpace);
        response.put("pagination", pagination);

        return ResponseEntity.ok(response);
    }

    /**
     * Get image
     */
    @GetMapping("/{storageSpace}/**")
    public ResponseEntity<Resource> getImage(
            @PathVariable("storageSpace") String storageSpace,
            HttpServletRequest request) {
        // Use AntPathMatcher to correctly extract the path after storageSpace
        String pattern = "/image/" + storageSpace + "/**";
        AntPathMatcher matcher = new AntPathMatcher();
        String path = matcher.extractPathWithinPattern(pattern, request.getRequestURI());

        // Validate path to prevent traversal attacks
        if (!SimplePicUtils.isPathSafe(path)) {
            logger.warn("Invalid path requested: {}", path);
            return ResponseEntity.badRequest().build();
        }

        // Normalize path
        path = SimplePicUtils.normalizePath(path);

        File imageFile = imageService.getImageFile(path, storageSpace);

        if (imageFile == null || !imageFile.exists()) {
            logger.warn("Image not found: storageSpace={}, path={}", storageSpace, path);
            return ResponseEntity.notFound().build();
        }

        String contentType = getContentType(imageFile.getName());

        // 检查是否启用了水印（全局开关 + 存储空间独立配置）
        StorageSpace space = storageService.getStorageSpace(storageSpace);
        WatermarkConfig wmConfig = (space != null) ? space.getWatermark() : null;
        Resource resource;

        boolean globalWatermarkEnabled = configService.getConfig().isWatermarkEnabled();
        if (globalWatermarkEnabled && wmConfig != null && wmConfig.isEnabled()) {
            File watermarkedFile = watermarkService.getWatermarkedImage(imageFile, wmConfig, storageSpace);
            resource = new FileSystemResource(watermarkedFile);
        } else {
            resource = new FileSystemResource(imageFile);
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=2592000")
                .body(resource);
    }

    /**
     * Get image info with dimensions
     */
    @GetMapping("/info/{storageSpace}/**")
    public ResponseEntity<Map<String, Object>> getImageInfo(
            @PathVariable("storageSpace") String storageSpace,
            HttpServletRequest request) {
        String pattern = "/image/info/" + storageSpace + "/**";
        AntPathMatcher matcher = new AntPathMatcher();
        String path = matcher.extractPathWithinPattern(pattern, request.getRequestURI());

        if (!SimplePicUtils.isPathSafe(path)) {
            return ResponseEntity.badRequest().build();
        }

        path = SimplePicUtils.normalizePath(path);
        File imageFile = imageService.getImageFile(path, storageSpace);

        if (imageFile == null || !imageFile.exists()) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> info = new HashMap<>();
        info.put("path", path);
        info.put("name", imageFile.getName());
        info.put("size", imageFile.length());
        info.put("lastModified", imageFile.lastModified());

        // Get image dimensions
        try {
            java.awt.image.BufferedImage bufferedImage = javax.imageio.ImageIO.read(imageFile);
            if (bufferedImage != null) {
                info.put("width", bufferedImage.getWidth());
                info.put("height", bufferedImage.getHeight());
                bufferedImage.flush();
            }
        } catch (IOException e) {
            logger.warn("Failed to read image dimensions for: {}", path);
        }

        return ResponseEntity.ok(info);
    }

    /**
     * Delete image
     */
    @DeleteMapping("/{storageSpace}/**")
    public ResponseEntity<Map<String, Object>> deleteImage(
            @PathVariable("storageSpace") String storageSpace,
            HttpServletRequest request) {
        // Use AntPathMatcher to correctly extract the path after storageSpace
        String pattern = "/image/" + storageSpace + "/**";
        AntPathMatcher matcher = new AntPathMatcher();
        String path = matcher.extractPathWithinPattern(pattern, request.getRequestURI());

        boolean success = imageService.deleteImage(path, storageSpace);

        if (success) {
            return ResponseEntity.ok(ResponseUtils.success());
        } else {
            return ResponseEntity.badRequest().body(ResponseUtils.error("failed_to_delete_image"));
        }
    }

    /**
     * Batch delete images
     */
    @DeleteMapping("/batch")
    public ResponseEntity<Map<String, Object>> batchDeleteImages(
            @RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        java.util.List<String> paths = (java.util.List<String>) request.get("paths");
        String storageSpace = (String) request.get("storageSpace");

        if (paths == null || paths.isEmpty()) {
            return ResponseEntity.badRequest().body(ResponseUtils.error("failed_to_delete_image"));
        }

        int successCount = 0;
        int failCount = 0;

        for (String path : paths) {
            if (imageService.deleteImage(path, storageSpace)) {
                successCount++;
            } else {
                failCount++;
            }
        }

        Map<String, Object> response = ResponseUtils.success();
        response.put("success", failCount == 0);
        response.put("successCount", successCount);
        response.put("failCount", failCount);

        if (failCount > 0) {
            response.put("error", ErrorMessages.getZh("failed_to_delete_image"));
            response.put("error_en", ErrorMessages.getEn("failed_to_delete_image"));
        }

        return ResponseEntity.ok(response);
    }

    private Map<String, Object> resultToMap(UploadResult result) {
        Map<String, Object> map = new HashMap<>();
        map.put("success", result.isSuccess());
        map.put("message", result.getMessage());
        map.put("url", result.getUrl());
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