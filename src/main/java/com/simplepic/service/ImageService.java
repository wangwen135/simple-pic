package com.simplepic.service;

import com.simplepic.model.ImageInfo;
import com.simplepic.model.StorageSpace;
import com.simplepic.model.SystemConfig;
import com.simplepic.model.UploadResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Image service
 * 图片服务
 */
@Service
public class ImageService {

    private static final Logger logger = LoggerFactory.getLogger(ImageService.class);

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "gif", "webp", "svg");

    @Autowired
    private StorageService storageService;

    @Autowired
    private ThumbnailService thumbnailService;

    @Autowired
    private ConfigService configService;

    @Value("${spring.servlet.multipart.max-file-size:10MB}")
    private String maxFileSize;

    /**
     * Upload image
     */
    public UploadResult uploadImage(MultipartFile file, String storageSpace) throws IOException {
        // Validate file
        if (file.isEmpty()) {
            return UploadResult.error("File is empty");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            return UploadResult.error("Invalid filename");
        }

        String extension = getExtension(originalFilename);
        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            return UploadResult.error("File type not allowed. Allowed types: " + String.join(", ", ALLOWED_EXTENSIONS));
        }

        // Check file size
        long fileSize = file.getSize();
        if (!hasEnoughSpace(storageSpace, fileSize)) {
            return UploadResult.error("Storage space quota exceeded");
        }

        // Get storage space
        com.simplepic.model.StorageSpace space = storageService.getStorageSpace(storageSpace);
        if (space == null) {
            return UploadResult.error("Storage space not found");
        }

        // Generate file path: yyyy/MM/UUID.ext
        SimpleDateFormat yearFormat = new SimpleDateFormat("yyyy");
        SimpleDateFormat monthFormat = new SimpleDateFormat("MM");
        Date now = new Date();

        String year = yearFormat.format(now);
        String month = monthFormat.format(now);
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String filename = uuid + "." + extension;

        String relativePath = year + File.separator + month + File.separator + filename;
        File targetDir = new File(space.getStorageDirectory(), year + File.separator + month);
        if (!targetDir.exists()) {
            targetDir.mkdirs();
        }

        File targetFile = new File(targetDir, filename);

        // Save file
        file.transferTo(targetFile);

        // Apply watermark if enabled
        SystemConfig config = configService.getConfig();
        if (config != null && config.isWatermarkEnabled()) {
            try {
                applyWatermark(targetFile, config);
            } catch (Exception e) {
                logger.warn("Failed to apply watermark", e);
            }
        }

        // Generate thumbnail
        try {
            thumbnailService.generateThumbnail(targetFile, storageSpace);
        } catch (Exception e) {
            logger.warn("Failed to generate thumbnail", e);
        }

        // Generate URLs
        String domain = space.getDomain().replaceAll("/$", "");
        String imageUrl = domain + "/api/image/" + storageSpace + "/" + relativePath.replace(File.separator, "/");
        String thumbnailUrl = domain + "/api/image/thumb/" + storageSpace + "/" + relativePath.replace(File.separator, "/");

        // Clear storage stats cache
        storageService.clearStatsCache(storageSpace);

        logger.info("Image uploaded: {} ({} bytes)", relativePath, fileSize);

        return UploadResult.success(imageUrl, thumbnailUrl, relativePath.replace(File.separator, "/"), storageSpace);
    }

    /**
     * Upload image from file (for admin backend)
     */
    public UploadResult uploadImage(File file, String storageSpace) throws IOException {
        if (!file.exists()) {
            return UploadResult.error("File not found");
        }

        String originalFilename = file.getName();
        String extension = getExtension(originalFilename);
        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            return UploadResult.error("File type not allowed");
        }

        long fileSize = file.length();
        if (!hasEnoughSpace(storageSpace, fileSize)) {
            return UploadResult.error("Storage space quota exceeded");
        }

        com.simplepic.model.StorageSpace space = storageService.getStorageSpace(storageSpace);
        if (space == null) {
            return UploadResult.error("Storage space not found");
        }

        // Generate file path
        SimpleDateFormat yearFormat = new SimpleDateFormat("yyyy");
        SimpleDateFormat monthFormat = new SimpleDateFormat("MM");
        Date now = new Date();

        String year = yearFormat.format(now);
        String month = monthFormat.format(now);
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String filename = uuid + "." + extension;

        String relativePath = year + File.separator + month + File.separator + filename;
        File targetDir = new File(space.getStorageDirectory(), year + File.separator + month);
        if (!targetDir.exists()) {
            targetDir.mkdirs();
        }

        File targetFile = new File(targetDir, filename);

        // Copy file using Java NIO
        java.nio.file.Files.copy(file.toPath(), targetFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);

        // Apply watermark
        SystemConfig config = configService.getConfig();
        if (config != null && config.isWatermarkEnabled()) {
            try {
                applyWatermark(targetFile, config);
            } catch (Exception e) {
                logger.warn("Failed to apply watermark", e);
            }
        }

        // Generate thumbnail
        try {
            thumbnailService.generateThumbnail(targetFile, storageSpace);
        } catch (Exception e) {
            logger.warn("Failed to generate thumbnail", e);
        }

        String domain = space.getDomain().replaceAll("/$", "");
        String imageUrl = domain + "/api/image/" + storageSpace + "/" + relativePath.replace(File.separator, "/");
        String thumbnailUrl = domain + "/api/image/thumb/" + storageSpace + "/" + relativePath.replace(File.separator, "/");

        storageService.clearStatsCache(storageSpace);

        logger.info("Image uploaded (backend): {} ({} bytes)", relativePath, fileSize);

        return UploadResult.success(imageUrl, thumbnailUrl, relativePath.replace(File.separator, "/"), storageSpace);
    }

    /**
     * Delete image
     */
    public boolean deleteImage(String path, String storageSpace) {
        com.simplepic.model.StorageSpace space = storageService.getStorageSpace(storageSpace);
        if (space == null) {
            return false;
        }

        File imageFile = new File(space.getStorageDirectory(), path.replace("/", File.separator));
        if (imageFile.exists()) {
            if (imageFile.delete()) {
                // Also delete thumbnail
                thumbnailService.deleteThumbnail(path, storageSpace);
                storageService.clearStatsCache(storageSpace);
                logger.info("Image deleted: {}", path);
                return true;
            }
        }

        return false;
    }

    /**
     * Get image file
     */
    public File getImageFile(String path, String storageSpace) {
        com.simplepic.model.StorageSpace space = storageService.getStorageSpace(storageSpace);
        if (space == null) {
            return null;
        }

        File imageFile = new File(space.getStorageDirectory(), path.replace("/", File.separator));
        if (imageFile.exists()) {
            return imageFile;
        }

        return null;
    }

    /**
     * List images in directory
     */
    public List<ImageInfo> listImages(String path, String storageSpace, boolean recursive) {
        com.simplepic.model.StorageSpace space = storageService.getStorageSpace(storageSpace);
        if (space == null) {
            return new ArrayList<>();
        }

        File baseDir = space.getStorageDirectory();
        File targetDir = path == null || path.isEmpty() ? baseDir : new File(baseDir, path.replace("/", File.separator));

        if (!targetDir.exists() || !targetDir.isDirectory()) {
            return new ArrayList<>();
        }

        List<ImageInfo> images = new ArrayList<>();

        if (recursive) {
            // Recursively find all images
            images = findAllImages(targetDir, baseDir, storageSpace);
        } else {
            // List images in current directory only
            File[] files = targetDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && isImageFile(file.getName())) {
                        String relativePath = getRelativePath(file, baseDir);
                        images.add(new ImageInfo(file, storageSpace, relativePath.replace(File.separator, "/")));
                    }
                }
            }
        }

        return images.stream()
                .sorted((a, b) -> Long.compare(b.getLastModified(), a.getLastModified()))
                .collect(Collectors.toList());
    }

    /**
     * List directories
     */
    public List<String> listDirectories(String path, String storageSpace) {
        com.simplepic.model.StorageSpace space = storageService.getStorageSpace(storageSpace);
        if (space == null) {
            return new ArrayList<>();
        }

        File baseDir = space.getStorageDirectory();
        File targetDir = path == null || path.isEmpty() ? baseDir : new File(baseDir, path.replace("/", File.separator));

        if (!targetDir.exists() || !targetDir.isDirectory()) {
            return new ArrayList<>();
        }

        List<String> directories = new ArrayList<>();
        File[] files = targetDir.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.isDirectory() && !file.getName().equals(".thumbnails")) {
                    String relativePath = getRelativePath(file, baseDir);
                    directories.add(relativePath.replace(File.separator, "/"));
                }
            }
        }

        directories.sort(String::compareTo);
        return directories;
    }

    /**
     * Create directory
     */
    public boolean createDirectory(String path, String storageSpace) {
        com.simplepic.model.StorageSpace space = storageService.getStorageSpace(storageSpace);
        if (space == null) {
            return false;
        }

        File newDir = new File(space.getStorageDirectory(), path.replace("/", File.separator));
        if (!newDir.exists()) {
            if (newDir.mkdirs()) {
                logger.info("Directory created: {}", path);
                return true;
            }
        }

        return false;
    }

    /**
     * Delete directory
     */
    public boolean deleteDirectory(String path, String storageSpace) {
        com.simplepic.model.StorageSpace space = storageService.getStorageSpace(storageSpace);
        if (space == null) {
            return false;
        }

        File dir = new File(space.getStorageDirectory(), path.replace("/", File.separator));
        if (dir.exists() && dir.isDirectory()) {
            // Delete all thumbnails first
            deleteThumbnailsRecursive(dir, space);

            // Delete directory
            if (deleteDirectoryRecursive(dir)) {
                storageService.clearStatsCache(storageSpace);
                logger.info("Directory deleted: {}", path);
                return true;
            }
        }

        return false;
    }

    /**
     * Apply watermark to image
     */
    private void applyWatermark(File imageFile, SystemConfig config) throws IOException {
        String extension = getExtension(imageFile.getName()).toLowerCase();
        if ("svg".equals(extension)) {
            return; // SVG not supported for watermark
        }

        BufferedImage image = ImageIO.read(imageFile);
        if (image == null) {
            return;
        }

        Graphics2D g2d = (Graphics2D) image.getGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if ("text".equals(config.getWatermarkType())) {
            applyTextWatermark(g2d, image, config);
        }

        g2d.dispose();
        ImageIO.write(image, extension, imageFile);
    }

    /**
     * Apply text watermark
     */
    private void applyTextWatermark(Graphics2D g2d, BufferedImage image, SystemConfig config) {
        String text = config.getWatermarkContent();
        float opacity = (float) config.getWatermarkOpacity();

        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 20));

        FontMetrics metrics = g2d.getFontMetrics();
        int textWidth = metrics.stringWidth(text);
        int textHeight = metrics.getHeight();

        int x, y;
        String position = config.getWatermarkPosition();

        switch (position) {
            case "top-left":
                x = 10;
                y = textHeight;
                break;
            case "top-right":
                x = image.getWidth() - textWidth - 10;
                y = textHeight;
                break;
            case "bottom-left":
                x = 10;
                y = image.getHeight() - 10;
                break;
            case "bottom-right":
                x = image.getWidth() - textWidth - 10;
                y = image.getHeight() - 10;
                break;
            case "center":
                x = (image.getWidth() - textWidth) / 2;
                y = (image.getHeight() - textHeight) / 2 + metrics.getAscent();
                break;
            default:
                x = image.getWidth() - textWidth - 10;
                y = image.getHeight() - 10;
        }

        g2d.drawString(text, x, y);
    }

    /**
     * Find all images recursively
     */
    private List<ImageInfo> findAllImages(File dir, File baseDir, String storageSpace) {
        List<ImageInfo> images = new ArrayList<>();
        File[] files = dir.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.isFile() && isImageFile(file.getName())) {
                    String relativePath = getRelativePath(file, baseDir);
                    images.add(new ImageInfo(file, storageSpace, relativePath.replace(File.separator, "/")));
                } else if (file.isDirectory() && !file.getName().equals(".thumbnails")) {
                    images.addAll(findAllImages(file, baseDir, storageSpace));
                }
            }
        }

        return images;
    }

    /**
     * Delete thumbnails recursively
     */
    private void deleteThumbnailsRecursive(File dir, com.simplepic.model.StorageSpace space) {
        File thumbnailsDir = space.getThumbnailsDirectory();
        File[] files = dir.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.isFile() && isImageFile(file.getName())) {
                    String relativePath = getRelativePath(file, space.getStorageDirectory());
                    thumbnailService.deleteThumbnail(relativePath.replace(File.separator, "/"), space.getName());
                } else if (file.isDirectory()) {
                    deleteThumbnailsRecursive(file, space);
                }
            }
        }
    }

    /**
     * Delete directory recursively
     */
    private boolean deleteDirectoryRecursive(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectoryRecursive(file);
                } else {
                    file.delete();
                }
            }
        }
        return dir.delete();
    }

    /**
     * Get file extension
     */
    private String getExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot > 0 && lastDot < filename.length() - 1) {
            return filename.substring(lastDot + 1);
        }
        return "";
    }

    /**
     * Check if file is an image
     */
    private boolean isImageFile(String filename) {
        String lower = filename.toLowerCase();
        return ALLOWED_EXTENSIONS.contains(lower.substring(lower.lastIndexOf('.') + 1));
    }

    /**
     * Get relative path
     */
    private String getRelativePath(File file, File baseDir) {
        String basePath = baseDir.getAbsolutePath();
        String filePath = file.getAbsolutePath();

        if (filePath.startsWith(basePath)) {
            return filePath.substring(basePath.length() + 1);
        }

        return file.getName();
    }

    /**
     * Check if storage has enough space
     */
    private boolean hasEnoughSpace(String storageSpace, long fileSize) {
        return storageService.hasEnoughSpace(storageSpace, fileSize);
    }

    /**
     * Get storage space
     */
    public StorageSpace getStorageSpace(String name) {
        return storageService.getStorageSpace(name);
    }

    /**
     * Get all storage spaces
     */
    public List<StorageSpace> getAllStorageSpaces() {
        return storageService.getAllStorageSpaces();
    }
}