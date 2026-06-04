package com.wwh.simplepic.interceptor;

import com.wwh.simplepic.model.LoginSession;
import com.wwh.simplepic.model.SystemConfig;
import com.wwh.simplepic.service.AuthService;
import com.wwh.simplepic.service.ConfigService;
import com.wwh.simplepic.util.ErrorMessages;
import com.wwh.simplepic.util.StorageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 认证拦截器
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(AuthInterceptor.class);

    @Autowired
    private AuthService authService;

    @Autowired
    private ConfigService configService;

    @Autowired
    private StorageUtils storageUtils;

    // Paths that don't require authentication
    private static final String[] PUBLIC_PATHS = {
            "/api/auth/login",
            "/api/auth/logout",
            "/api/auth/config",
            "/login.html",
            "/api/image/",
            "/image/upload",
            "/favicon.svg",
            "/favicon.ico",
            "/lib/",
            "/css/",
            "/js/"
    };

    // Paths that require admin role
    private static final String[] ADMIN_PATHS = {
            "/admin/dashboard.html",
            "/admin/images.html",
            "/admin/storage.html",
            "/admin/users.html",
            "/admin/system.html",
            "/admin/apikey.html",
            "/api/admin/"
    };

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI();
        String method = request.getMethod();

        logger.debug("Request: {} {}", method, path);

        // Handle root path specially
        if ("/".equals(path)) {
            String token = getTokenFromRequest(request);
            LoginSession session = token != null ? authService.getSession(token) : null;

            if (session == null) {
                // Not logged in, check if anonymous upload is available
                SystemConfig config = configService.getConfig();
                boolean canAnonymousUpload = config != null && config.isAnonymousUploadEnabled()
                        && storageUtils.findAnonymousUploadSpace() != null;

                if (canAnonymousUpload) {
                    response.sendRedirect("/upload.html");
                } else {
                    response.sendRedirect("/login.html");
                }
            } else {
                response.sendRedirect("/upload.html");
            }
            return false;
        }

        // Check if path is public
        if (isPublicPath(path) || isPublicImageRequest(path, method)) {
            return true;
        }

        // Handle /upload.html specially - check if anonymous upload is enabled
        if ("/upload.html".equals(path)) {
            String token = getTokenFromRequest(request);
            LoginSession session = token != null ? authService.getSession(token) : null;

            if (session == null) {
                // Not logged in, check if anonymous upload is enabled
                SystemConfig config = configService.getConfig();
                if (config == null || !config.isAnonymousUploadEnabled() || storageUtils.findAnonymousUploadSpace() == null) {
                    // Anonymous upload not enabled, redirect to login
                    response.sendRedirect("/login.html");
                    return false;
                }
                // Anonymous upload enabled, allow access
                return true;
            }
            // Logged in, allow access
            return true;
        }

        // Get session token from cookie
        String token = getTokenFromRequest(request);

        if (token == null) {
            // Redirect to login page for HTML requests
            if (path.endsWith(".html")) {
                response.sendRedirect("/login.html");
                return false;
            }
            // Return 401 for API requests
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\":\"" + ErrorMessages.getZh("unauthorized") + "\",\"error_en\":\"" + ErrorMessages.getEn("unauthorized") + "\"}");
            return false;
        }

        LoginSession session = authService.getSession(token);
        if (session == null) {
            if (path.endsWith(".html")) {
                response.sendRedirect("/login.html");
                return false;
            }
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\":\"" + ErrorMessages.getZh("invalid_or_expired_token") + "\",\"error_en\":\"" + ErrorMessages.getEn("invalid_or_expired_token") + "\"}");
            return false;
        }

        // Check admin role for admin paths and image management endpoints.
        if ((isAdminPath(path) || isImageManagementRequest(path, method)) && !"ADMIN".equals(session.getRole())) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\":\"" + ErrorMessages.getZh("forbidden") + "\",\"error_en\":\"" + ErrorMessages.getEn("forbidden") + "\"}");
            return false;
        }

        // Add session info to request
        request.setAttribute("session", session);

        return true;
    }

    /**
     * 检查路径是否为公共路径（不需要认证）
     */
    private boolean isPublicPath(String path) {
        for (String publicPath : PUBLIC_PATHS) {
            if (path.startsWith(publicPath)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 公开图片访问只允许读取真实图片文件，图片管理接口仍然需要认证。
     */
    private boolean isPublicImageRequest(String path, String method) {
        if (!"GET".equalsIgnoreCase(method) && !"HEAD".equalsIgnoreCase(method)) {
            return false;
        }
        if (!path.startsWith("/image/")) {
            return false;
        }
        return !path.equals("/image/list")
                && !path.startsWith("/image/list/")
                && !path.equals("/image/batch")
                && !path.startsWith("/image/batch/")
                && !path.startsWith("/image/info/");
    }

    /**
     * 图片列表、删除等管理接口要求管理员权限。
     */
    private boolean isImageManagementRequest(String path, String method) {
        if (path.equals("/image/list") || path.startsWith("/image/list/")) {
            return true;
        }
        if (path.equals("/image/batch") || path.startsWith("/image/batch/")) {
            return true;
        }
        if (path.startsWith("/image/info/")) {
            return true;
        }
        return "DELETE".equalsIgnoreCase(method) && path.startsWith("/image/");
    }

    /**
     * 检查路径是否需要管理员角色
     */
    private boolean isAdminPath(String path) {
        for (String adminPath : ADMIN_PATHS) {
            if (path.startsWith(adminPath)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 从请求中提取认证令牌（优先级：Cookie > Authorization header > 查询参数）
     */
    private String getTokenFromRequest(HttpServletRequest request) {
        // 优先从 Cookie 获取
        javax.servlet.http.Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (javax.servlet.http.Cookie cookie : cookies) {
                if ("token".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        // 检查 Authorization header
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            return token.substring(7);
        }

        // 检查查询参数
        token = request.getParameter("token");
        if (token != null) {
            return token;
        }

        return null;
    }
}
