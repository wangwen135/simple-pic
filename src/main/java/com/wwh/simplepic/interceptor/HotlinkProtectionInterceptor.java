package com.wwh.simplepic.interceptor;

import com.wwh.simplepic.model.SystemConfig;
import com.wwh.simplepic.service.ConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.OutputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 防盗链拦截器
 * 通过检查 HTTP Referer 头判断请求来源是否在白名单中
 */
@Component
public class HotlinkProtectionInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(HotlinkProtectionInterceptor.class);

    @Autowired
    private ConfigService configService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        SystemConfig config = configService.getConfig();
        if (config == null || !config.isHotlinkProtectionEnabled()) {
            return true;
        }

        String referer = request.getHeader("Referer");

        // 空 Referer（直接访问或书签）— 放行
        if (referer == null || referer.isEmpty()) {
            return true;
        }

        // 解析 Referer 域名
        String refererHost;
        try {
            URI uri = new URI(referer);
            refererHost = uri.getHost();
            if (refererHost == null) {
                return true;
            }
        } catch (Exception e) {
            // 无法解析的 Referer — 放行
            return true;
        }

        // 同源请求（Referer 与请求自身同域名）— 放行
        if (refererHost.equals(request.getServerName())) {
            return true;
        }

        // 检查白名单
        String allowedReferers = config.getAllowedReferers();
        if (allowedReferers != null && !allowedReferers.isEmpty()) {
            List<String> whitelist = parseWhitelist(allowedReferers);
            for (String allowed : whitelist) {
                if (allowed.isEmpty()) continue;
                // 支持通配符，如 *.example.com
                if (matchDomain(refererHost, allowed)) {
                    return true;
                }
            }
        }

        // 不在白名单中 — 根据配置处理
        logger.debug("Hotlink blocked: referer={}, host={}", referer, refererHost);
        String responseMode = config.getHotlinkResponse();
        if (responseMode == null) {
            responseMode = "generated";
        }

        switch (responseMode) {
            case "image":
                return serveCustomImage(config, response);
            case "403":
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("text/plain;charset=UTF-8");
                response.getWriter().write("403 Forbidden - Hotlink not allowed");
                return false;
            case "generated":
            default:
                return serveGeneratedImage(response);
        }
    }

    /**
     * 解析白名单字符串（支持逗号、换行分隔）
     */
    private List<String> parseWhitelist(String allowedReferers) {
        return Arrays.stream(allowedReferers.split("[,\\n\\r]+"))
                .map(String::trim)
                .map(s -> s.replaceAll("^https?://", ""))
                .map(s -> s.replaceAll("/.*$", ""))
                .collect(Collectors.toList());
    }

    /**
     * 域名匹配（支持通配符 *.example.com）
     */
    private boolean matchDomain(String host, String pattern) {
        if (pattern.startsWith("*.")) {
            String suffix = pattern.substring(2);
            return host.equals(suffix) || host.endsWith("." + suffix);
        }
        return host.equals(pattern);
    }

    /**
     * 动态生成防盗链提示图
     */
    private boolean serveGeneratedImage(HttpServletResponse response) throws Exception {
        int width = 400;
        int height = 200;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        // 开启抗锯齿
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // 白色背景
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);

        // 红色边框
        g.setColor(new Color(220, 53, 69));
        g.setStroke(new BasicStroke(3));
        g.drawRect(10, 10, width - 20, height - 20);

        // 中文主标题
        g.setColor(new Color(220, 53, 69));
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22));
        FontMetrics fm = g.getFontMetrics();
        String textCN = "\u9632\u76D7\u94FE\u56FE\u7247\u7981\u6B62\u5F15\u7528";
        g.drawString(textCN, (width - fm.stringWidth(textCN)) / 2, 70);

        // 英文主标题
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        fm = g.getFontMetrics();
        String textEN = "Hotlink Not Allowed";
        g.drawString(textEN, (width - fm.stringWidth(textEN)) / 2, 100);

        // 中文副标题
        g.setColor(Color.GRAY);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        fm = g.getFontMetrics();
        String subCN = "\u6B64\u56FE\u7247\u7981\u6B62\u5916\u90E8\u5F15\u7528";
        g.drawString(subCN, (width - fm.stringWidth(subCN)) / 2, 135);

        // 英文副标题
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        fm = g.getFontMetrics();
        String subEN = "This image is protected from hotlinking";
        g.drawString(subEN, (width - fm.stringWidth(subEN)) / 2, 160);

        g.dispose();

        response.setContentType("image/png");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        try (OutputStream os = response.getOutputStream()) {
            ImageIO.write(image, "png", os);
            os.flush();
        }
        return false;
    }

    /**
     * 返回自定义防盗链提示图
     */
    private boolean serveCustomImage(SystemConfig config, HttpServletResponse response) throws Exception {
        String imagePath = config.getHotlinkImagePath();
        if (imagePath == null || imagePath.isEmpty()) {
            // 没有自定义图片，回退到生成模式
            return serveGeneratedImage(response);
        }

        File imageFile = new File(imagePath);
        if (!imageFile.exists()) {
            return serveGeneratedImage(response);
        }

        String name = imageFile.getName().toLowerCase();
        String contentType;
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) {
            contentType = "image/jpeg";
        } else if (name.endsWith(".png")) {
            contentType = "image/png";
        } else if (name.endsWith(".gif")) {
            contentType = "image/gif";
        } else if (name.endsWith(".webp")) {
            contentType = "image/webp";
        } else {
            contentType = "image/png";
        }

        response.setContentType(contentType);
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        try (OutputStream os = response.getOutputStream()) {
            java.nio.file.Files.copy(imageFile.toPath(), os);
            os.flush();
        }
        return false;
    }
}
