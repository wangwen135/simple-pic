package com.wwh.simplepic.service;

import com.wwh.simplepic.model.WatermarkConfig;
import com.wwh.simplepic.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;

/**
 * 水印服务 - 动态生成带水印的图片，支持磁盘缓存
 */
@Service
public class WatermarkService {

    private static final Logger logger = LoggerFactory.getLogger(WatermarkService.class);

    @Autowired
    private StorageService storageService;

    /**
     * 获取带水印的图片文件（优先从缓存读取）
     *
     * @param originalFile   原始图片文件
     * @param config         水印配置
     * @param storageSpaceName 存储空间名称
     * @return 带水印的图片文件，如果不需要水印则返回原文件
     */
    public File getWatermarkedImage(File originalFile, WatermarkConfig config, String storageSpaceName) {
        if (config == null || !config.isEnabled()) {
            return originalFile;
        }

        String extension = getExtension(originalFile.getName()).toLowerCase();
        // SVG 和 GIF 动图不支持水印
        if ("svg".equals(extension) || "gif".equals(extension)) {
            return originalFile;
        }

        try {
            // 生成缓存键
            String cacheKey = generateCacheKey(originalFile, config);
            File cacheDir = getCacheDir(storageSpaceName);
            if (cacheDir == null) {
                return originalFile;
            }

            String cacheFilename = cacheKey + "." + extension;
            File cachedFile = new File(cacheDir, cacheFilename);

            // 缓存命中
            if (cachedFile.exists() && cachedFile.length() > 0) {
                return cachedFile;
            }

            // 生成带水印的图片
            if (!cacheDir.exists()) {
                cacheDir.mkdirs();
            }

            // 写入临时文件后原子重命名，避免并发问题
            File tempFile = new File(cacheDir, cacheFilename + ".tmp");
            applyWatermarkToFile(originalFile, tempFile, config, extension);

            // 原子重命名
            tempFile.renameTo(cachedFile);

            return cachedFile;
        } catch (Exception e) {
            logger.warn("生成水印图片失败，返回原图: {}", e.getMessage());
            return originalFile;
        }
    }

    /**
     * 清除指定存储空间的所有水印缓存
     */
    public void clearWatermarkCache(String storageSpaceName) {
        File cacheDir = getCacheDir(storageSpaceName);
        if (cacheDir != null && cacheDir.exists()) {
            File[] files = cacheDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    file.delete();
                }
            }
            logger.info("已清除存储空间 {} 的水印缓存", storageSpaceName);
        }
    }

    /**
     * 删除指定图片的水印缓存
     */
    public void deleteCachedWatermark(String relativePath, String storageSpaceName) {
        File cacheDir = getCacheDir(storageSpaceName);
        if (cacheDir == null || !cacheDir.exists()) {
            return;
        }

        String extension = getExtension(relativePath);
        String prefix = generateCacheKeyPrefix(relativePath);

        // 删除所有匹配该前缀的缓存文件（不同水印配置可能产生多个缓存）
        File[] files = cacheDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.getName().startsWith(prefix)) {
                    file.delete();
                }
            }
        }
    }

    /**
     * 获取水印缓存目录
     */
    private File getCacheDir(String storageSpaceName) {
        com.wwh.simplepic.model.StorageSpace space = storageService.getStorageSpace(storageSpaceName);
        if (space == null) {
            return null;
        }
        return new File(space.getStorageDirectory(), Constants.Directories.WATERMARKS);
    }

    /**
     * 生成缓存键: SHA-256(相对路径|文件修改时间|水印配置) 取前16位
     */
    private String generateCacheKey(File originalFile, WatermarkConfig config) throws Exception {
        // 使用文件名作为路径标识（因为 originalFile 已经是完整路径）
        String input = originalFile.getName() + "|" + originalFile.lastModified() + "|" + config.toHashString();
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            hex.append(String.format("%02x", hash[i]));
        }
        return hex.toString();
    }

    /**
     * 生成缓存键前缀（仅基于文件名，用于删除时匹配）
     */
    private String generateCacheKeyPrefix(String relativePath) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(relativePath.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (int i = 0; i < 4; i++) {
                hex.append(String.format("%02x", hash[i]));
            }
            return hex.toString();
        } catch (Exception e) {
            return relativePath.hashCode() + "";
        }
    }

    /**
     * 对图片文件应用水印并写入输出文件
     */
    private void applyWatermarkToFile(File imageFile, File outputFile, WatermarkConfig config, String extension) throws IOException {
        BufferedImage image = ImageIO.read(imageFile);
        if (image == null) {
            throw new IOException("无法读取图片: " + imageFile.getName());
        }

        Graphics2D g2d = (Graphics2D) image.getGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        if ("text".equals(config.getType())) {
            applyTextWatermark(g2d, image, config);
        }

        g2d.dispose();
        ImageIO.write(image, extension, outputFile);
    }

    /**
     * 应用文字水印
     */
    private void applyTextWatermark(Graphics2D g2d, BufferedImage image, WatermarkConfig config) {
        String text = config.getContent();
        if (text == null || text.isEmpty()) {
            return;
        }

        float opacity = (float) config.getOpacity();
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 20));

        FontMetrics metrics = g2d.getFontMetrics();
        int textWidth = metrics.stringWidth(text);
        int textHeight = metrics.getHeight();

        int x, y;
        String position = config.getPosition();
        int margin = 10;

        switch (position != null ? position : "bottom-right") {
            case "top-left":
                x = margin;
                y = textHeight;
                break;
            case "top-right":
                x = image.getWidth() - textWidth - margin;
                y = textHeight;
                break;
            case "bottom-left":
                x = margin;
                y = image.getHeight() - margin;
                break;
            case "center":
                x = (image.getWidth() - textWidth) / 2;
                y = (image.getHeight() - textHeight) / 2 + metrics.getAscent();
                break;
            case "bottom-right":
            default:
                x = image.getWidth() - textWidth - margin;
                y = image.getHeight() - margin;
        }

        g2d.drawString(text, x, y);
    }

    private String getExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot > 0 && lastDot < filename.length() - 1) {
            return filename.substring(lastDot + 1);
        }
        return "";
    }
}
