package com.wwh.simplepic.controller;

import com.wwh.simplepic.service.ImageService;
import com.wwh.simplepic.util.SimplePicUtils;
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

import javax.servlet.http.HttpServletRequest;
import java.io.File;

/**
 * Images controller - serves images at /images/{storageSpace}/**
 * Handles the plural /images/ path for backward compatibility.
 * Reads storage space paths dynamically from ConfigService on each request.
 */
@RestController
@RequestMapping("/images")
public class ImagesController {

    private static final Logger logger = LoggerFactory.getLogger(ImagesController.class);

    @Autowired
    private ImageService imageService;

    @GetMapping("/{storageSpace}/**")
    public ResponseEntity<Resource> getImage(
            @PathVariable("storageSpace") String storageSpace,
            HttpServletRequest request) {
        String pattern = "/images/" + storageSpace + "/**";
        AntPathMatcher matcher = new AntPathMatcher();
        String path = matcher.extractPathWithinPattern(pattern, request.getRequestURI());

        if (!SimplePicUtils.isPathSafe(path)) {
            logger.warn("Invalid path requested: {}", path);
            return ResponseEntity.badRequest().build();
        }

        path = SimplePicUtils.normalizePath(path);

        File imageFile = imageService.getImageFile(path, storageSpace);

        if (imageFile == null || !imageFile.exists()) {
            logger.warn("Image not found: storageSpace={}, path={}", storageSpace, path);
            return ResponseEntity.notFound().build();
        }

        String contentType = getContentType(imageFile.getName());
        Resource resource = new FileSystemResource(imageFile);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000")
                .body(resource);
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
