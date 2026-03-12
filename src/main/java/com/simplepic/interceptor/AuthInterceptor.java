package com.simplepic.interceptor;

import com.simplepic.model.LoginSession;
import com.simplepic.model.SystemConfig;
import com.simplepic.service.AuthService;
import com.simplepic.service.ConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Authentication interceptor
 * 认证拦截器
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(AuthInterceptor.class);

    @Autowired
    private AuthService authService;

    @Autowired
    private ConfigService configService;

    // Paths that don't require authentication
    private static final String[] PUBLIC_PATHS = {
            "/api/auth/login",
            "/api/auth/logout",
            "/api/auth/config",
            "/login.html",
            "/api/image/",
            "/admin/login.html"
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
                response.sendRedirect("/login.html");
            } else {
                response.sendRedirect("/upload.html");
            }
            return false;
        }

        // Check if path is public
        if (isPublicPath(path)) {
            return true;
        }

        // Handle /upload.html specially - check if anonymous upload is enabled
        if ("/upload.html".equals(path)) {
            String token = getTokenFromRequest(request);
            LoginSession session = token != null ? authService.getSession(token) : null;

            if (session == null) {
                // Not logged in, check if anonymous upload is enabled
                SystemConfig config = configService.getConfig();
                if (!config.isAnonymousUploadEnabled()) {
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
            response.getWriter().write("{\"error\":\"Unauthorized\"}");
            return false;
        }

        LoginSession session = authService.getSession(token);
        if (session == null) {
            if (path.endsWith(".html")) {
                response.sendRedirect("/login.html");
                return false;
            }
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\":\"Invalid or expired token\"}");
            return false;
        }

        // Check admin role for admin paths
        if (isAdminPath(path) && !"ADMIN".equals(session.getRole())) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("{\"error\":\"Forbidden: Admin access required\"}");
            return false;
        }

        // Add session info to request
        request.setAttribute("session", session);

        return true;
    }

    private boolean isPublicPath(String path) {
        for (String publicPath : PUBLIC_PATHS) {
            if (path.startsWith(publicPath)) {
                return true;
            }
        }
        return false;
    }

    private boolean isAdminPath(String path) {
        for (String adminPath : ADMIN_PATHS) {
            if (path.startsWith(adminPath)) {
                return true;
            }
        }
        return false;
    }

    private String getTokenFromRequest(HttpServletRequest request) {
        // Check cookie first
        javax.servlet.http.Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (javax.servlet.http.Cookie cookie : cookies) {
                if ("token".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        // Check header
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            return token.substring(7);
        }

        // Check query parameter
        token = request.getParameter("token");
        if (token != null) {
            return token;
        }

        return null;
    }
}