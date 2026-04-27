package com.wwh.simplepic.service;

import com.wwh.simplepic.model.ImageInfo;
import com.wwh.simplepic.model.StorageSpace;
import com.wwh.simplepic.model.SystemConfig;
import com.wwh.simplepic.model.UploadResult;
import com.wwh.simplepic.util.Constants;
import com.wwh.simplepic.util.ErrorMessages;
import com.wwh.simplepic.util.FileUtils;
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
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 图片服务
 */
@Service
public class ImageService {

    private static final Logger logger = LoggerFactory.getLogger(ImageService.class);

    @Autowired
    private StorageService storageService;

    @Autowired
    private ConfigService configService;

    @Value("${spring.servlet.multipart.max-file-size:10MB}")
    private String maxFileSize;

    /**
     * Upload image
     */
    public UploadResult uploadImage(MultipartFile file, String storageSpace) throws IOException {
        if (file.isEmpty()) {
            return UploadResult.error(ErrorMessages.getZh("file_is_empty"));
        }

        return doUploadImage(file.getInputStream(), file.getOriginalFilename(), file.getSize(), storageSpace, null);
    }

    /**
     * Upload image to custom path
     */
    public UploadResult uploadImageToPath(MultipartFile file, String storageSpace, String targetPath) throws IOException {
        if (file.isEmpty()) {
            return UploadResult.error(ErrorMessages.getZh("file_is_empty"));
        }

        return doUploadImage(file.getInputStream(), file.getOriginalFilename(), file.getSize(), storageSpace, targetPath);
    }

    /**
     * Common upload logic with optional target path
     */
    private UploadResult doUploadImage(InputStream inputStream, String originalFilename, long fileSize, String storageSpace, String targetPath) throws IOException {
        // Validate filename
        if (originalFilename == null) {
            return UploadResult.error(ErrorMessages.getZh("invalid_filename"));
        }

        String extension = FileUtils.getFileExtension(originalFilename);
        if (!Constants.Extensions.IMAGES.contains(extension.toLowerCase())) {
            return UploadResult.error(ErrorMessages.getZh("file_type_not_allowed") + ": " + String.join(", ", Constants.Extensions.IMAGES));
        }

        // Check file size
        if (!hasEnoughSpace(storageSpace, fileSize)) {
            return UploadResult.error(ErrorMessages.getZh("storage_quota_exceeded"));
        }

        // Get storage space
        com.wwh.simplepic.model.StorageSpace space = storageService.getStorageSpace(storageSpace);
        if (space == null) {
            return UploadResult.error(ErrorMessages.getZh("storage_space_not_found"));
        }

        // Generate file path
        // If targetPath is specified, use it; otherwise use yyyy/MM structure
        String relativePath;
        File targetDir;

        if (targetPath != null) {
            // Use custom path (empty string means root directory)
            String uuid = UUID.randomUUID().toString().replace("-", "");
            String filename = uuid + "." + extension;
            if (targetPath.isEmpty()) {
                relativePath = filename;
                targetDir = space.getStorageDirectory();
            } else {
                relativePath = targetPath.replace("/", File.separator) + File.separator + filename;
                targetDir = new File(space.getStorageDirectory(), targetPath.replace("/", File.separator));
            }
        } else {
            // Use default yyyy/MM structure
            SimpleDateFormat yearFormat = new SimpleDateFormat("yyyy");
            SimpleDateFormat monthFormat = new SimpleDateFormat("MM");
            Date now = new Date();

            String year = yearFormat.format(now);
            String month = monthFormat.format(now);
            String uuid = UUID.randomUUID().toString().replace("-", "");
            String filename = uuid + "." + extension;

            relativePath = year + File.separator + month + File.separator + filename;
            targetDir = new File(space.getStorageDirectory(), year + File.separator + month);
        }

        if (!targetDir.exists()) {
            targetDir.mkdirs();
        }

        String filename = relativePath.substring(relativePath.lastIndexOf(File.separator) + 1);
        File targetFile = new File(targetDir, filename);

        // Save file
        java.nio.file.Files.copy(inputStream, targetFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);

        // Apply watermark if enabled
        SystemConfig config = configService.getConfig();
        if (config != null && config.isWatermarkEnabled()) {
            try {
                applyWatermark(targetFile, config);
            } catch (IOException e) {
                logger.warn("Failed to apply watermark", e);
            }
        }

        // Generate URLs: urlPrefix + relativePath
        String urlPrefix = space.getUrlPrefix();
        if (urlPrefix == null || urlPrefix.isEmpty()) {
            // Use default image path when urlPrefix is not configured
            urlPrefix = "/image/" + storageSpace;
        } else {
            urlPrefix = urlPrefix.replaceAll("/$", "");
        }

        String relativePathUrl = relativePath.replace(File.separator, "/");
        String imageUrl = urlPrefix + "/" + relativePathUrl;

        // Clear storage stats cache
        storageService.clearStatsCache(storageSpace);

        logger.info("Image uploaded: {} ({} bytes)", relativePath, fileSize);

        return UploadResult.success(imageUrl, relativePath.replace(File.separator, "/"), storageSpace);
    }

    /**
     * Upload image from file (for admin backend)
     */
    public UploadResult uploadImage(File file, String storageSpace) throws IOException {
        if (!file.exists()) {
            return UploadResult.error(ErrorMessages.getZh("file_not_found"));
        }

        try (java.io.FileInputStream fis = new java.io.FileInputStream(file)) {
            return doUploadImage(fis, file.getName(), file.length(), storageSpace, null);
        }
    }

    /**
     * Delete image
     */
    public boolean deleteImage(String path, String storageSpace) {
        com.wwh.simplepic.model.StorageSpace space = storageService.getStorageSpace(storageSpace);
        if (space == null) {
            return false;
        }

        File imageFile = FileUtils.validatePath(path, space.getStorageDirectory());
        if (imageFile == null) {
            logger.warn("Invalid path for deletion: {}", path);
            return false;
        }

        if (imageFile.exists()) {
            if (imageFile.delete()) {
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
        com.wwh.simplepic.model.StorageSpace space = storageService.getStorageSpace(storageSpace);
        if (space == null) {
            return null;
        }

        File imageFile = FileUtils.validatePath(path, space.getStorageDirectory());
        if (imageFile == null) {
            return null;
        }

        if (imageFile.exists()) {
            return imageFile;
        }

        return null;
    }

    /**
     * List images in directory
     */
    public List<ImageInfo> listImages(String path, String storageSpace, boolean recursive) {
        com.wwh.simplepic.model.StorageSpace space = storageService.getStorageSpace(storageSpace);
        if (space == null) {
            return new ArrayList<>();
        }

        File baseDir = space.getStorageDirectory();
        File targetDir = baseDir;
        if (path != null && !path.isEmpty()) {
            targetDir = FileUtils.validatePath(path, baseDir);
            if (targetDir == null) {
                return new ArrayList<>();
            }
        }

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
                    if (file.isFile() && FileUtils.isImageFile(file.getName())) {
                        String relativePath = FileUtils.getRelativePath(file, baseDir);
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
        com.wwh.simplepic.model.StorageSpace space = storageService.getStorageSpace(storageSpace);
        if (space == null) {
            return new ArrayList<>();
        }

        File baseDir = space.getStorageDirectory();
        File targetDir = baseDir;
        if (path != null && !path.isEmpty()) {
            targetDir = FileUtils.validatePath(path, baseDir);
            if (targetDir == null) {
                return new ArrayList<>();
            }
        }

        if (!targetDir.exists() || !targetDir.isDirectory()) {
            return new ArrayList<>();
        }

        List<String> directories = new ArrayList<>();
        File[] files = targetDir.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.isDirectory() && !file.getName().equals(Constants.Directories.THUMBNAILS)) {
                    String relativePath = FileUtils.getRelativePath(file, baseDir);
                    directories.add(relativePath.replace(File.separator, "/"));
                }
            }
        }

        directories.sort(String::compareTo);
        return directories;
    }

    /**
     * Rename image file
     */
    public boolean renameImage(String oldPath, String newPath, String storageSpace) {
        com.wwh.simplepic.model.StorageSpace space = storageService.getStorageSpace(storageSpace);
        if (space == null) {
            return false;
        }

        File oldFile = FileUtils.validatePath(oldPath, space.getStorageDirectory());
        File newFile = FileUtils.validatePath(newPath, space.getStorageDirectory());
        if (oldFile == null || newFile == null) {
            logger.warn("Invalid path for rename: {} -> {}", oldPath, newPath);
            return false;
        }

        if (oldFile.exists() && oldFile.isFile() && !newFile.exists()) {
            // Ensure parent directory exists
            File parentDir = newFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            if (oldFile.renameTo(newFile)) {
                storageService.clearStatsCache(storageSpace);
                logger.info("Image renamed: {} -> {}", oldPath, newPath);
                return true;
            }
        }

        return false;
    }

    /**
     * Move image to different directory
     */
    public boolean moveImage(String sourcePath, String targetDirPath, String storageSpace) {
        com.wwh.simplepic.model.StorageSpace space = storageService.getStorageSpace(storageSpace);
        if (space == null) {
            return false;
        }

        File sourceFile = FileUtils.validatePath(sourcePath, space.getStorageDirectory());
        if (sourceFile == null) {
            logger.warn("Invalid source path for move: {}", sourcePath);
            return false;
        }

        String fileName = sourceFile.getName();
        File targetDir = targetDirPath == null || targetDirPath.isEmpty() ?
            space.getStorageDirectory() :
            FileUtils.validatePath(targetDirPath, space.getStorageDirectory());

        if (targetDir == null) {
            logger.warn("Invalid target directory path for move: {}", targetDirPath);
            return false;
        }

        File targetFile = new File(targetDir, fileName);

        if (sourceFile.exists() && sourceFile.isFile()) {
            // Ensure target directory exists
            if (!targetDir.exists()) {
                targetDir.mkdirs();
            }

            if (sourceFile.renameTo(targetFile)) {
                storageService.clearStatsCache(storageSpace);
                logger.info("Image moved: {} -> {}/{}", sourcePath, targetDirPath, fileName);
                return true;
            }
        }

        return false;
    }

    /**
     * Rename directory
     */
    public boolean renameDirectory(String oldPath, String newPath, String storageSpace) {
        com.wwh.simplepic.model.StorageSpace space = storageService.getStorageSpace(storageSpace);
        if (space == null) {
            return false;
        }

        File oldDir = FileUtils.validatePath(oldPath, space.getStorageDirectory());
        File newDir = FileUtils.validatePath(newPath, space.getStorageDirectory());
        if (oldDir == null || newDir == null) {
            logger.warn("Invalid path for directory rename: {} -> {}", oldPath, newPath);
            return false;
        }

        if (oldDir.exists() && oldDir.isDirectory() && !newDir.exists()) {
            if (oldDir.renameTo(newDir)) {
                logger.info("Directory renamed: {} -> {}", oldPath, newPath);
                return true;
            }
        }

        return false;
    }

    /**
     * Create directory
     */
    public boolean createDirectory(String path, String storageSpace) {
        com.wwh.simplepic.model.StorageSpace space = storageService.getStorageSpace(storageSpace);
        if (space == null) {
            return false;
        }

        File newDir = FileUtils.validatePath(path, space.getStorageDirectory());
        if (newDir == null) {
            logger.warn("Invalid path for directory creation: {}", path);
            return false;
        }

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
        com.wwh.simplepic.model.StorageSpace space = storageService.getStorageSpace(storageSpace);
        if (space == null) {
            return false;
        }

        File dir = FileUtils.validatePath(path, space.getStorageDirectory());
        if (dir == null) {
            logger.warn("Invalid path for directory deletion: {}", path);
            return false;
        }

        if (dir.exists() && dir.isDirectory()) {
            if (FileUtils.deleteDirectoryRecursive(dir)) {
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
                if (file.isFile() && FileUtils.isImageFile(file.getName())) {
                    String relativePath = FileUtils.getRelativePath(file, baseDir);
                    images.add(new ImageInfo(file, storageSpace, relativePath.replace(File.separator, "/")));
                } else if (file.isDirectory() && !file.getName().equals(Constants.Directories.THUMBNAILS)) {
                    images.addAll(findAllImages(file, baseDir, storageSpace));
                }
            }
        }

        return images;
    }

    /**
     * Get file extension
     */
    private String getExtension(String filename) {
        return FileUtils.getFileExtension(filename);
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