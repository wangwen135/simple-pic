package com.wwh.simplepic.interceptor;

import com.wwh.simplepic.model.SystemConfig;
import com.wwh.simplepic.security.RateLimiter;
import com.wwh.simplepic.service.ConfigService;
import com.wwh.simplepic.util.ErrorMessages;
import com.wwh.simplepic.util.SimplePicUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Rate limit interceptor
 * 限流拦截器
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitInterceptor.class);

    @Autowired
    private RateLimiter rateLimiter;

    @Autowired
    private ConfigService configService;

    // Paths that are rate limited
    private static final String[] RATE_LIMITED_PATHS = {
            "/api/image/upload",
            "/api/admin/"
    };

    private int maxRequests = 100;
    private int timeWindow = 60;

    /**
     * Initialize rate limit configuration from system config
     * 从系统配置初始化限流配置
     */
    @PostConstruct
    public void init() {
        loadRateLimitConfig();
    }

    /**
     * Load rate limit configuration from system config
     * 从系统配置加载限流配置
     */
    private void loadRateLimitConfig() {
        SystemConfig config = configService.getConfig();
        if (config != null && config.isRateLimitEnabled()) {
            this.maxRequests = config.getMaxRequests() > 0 ? config.getMaxRequests() : 100;
            this.timeWindow = config.getTimeWindow() > 0 ? config.getTimeWindow() : 60;
            logger.info("Rate limit configured: {} requests per {} seconds", maxRequests, timeWindow);
        }
    }

    /**
     * Set rate limit configuration (for programmatic updates)
     * 设置限流配置（用于程序化更新）
     */
    public void setRateLimitConfig(int maxRequests, int timeWindow) {
        this.maxRequests = maxRequests;
        this.timeWindow = timeWindow;
        logger.info("Rate limit updated: {} requests per {} seconds", maxRequests, timeWindow);
    }

    /**
     * Reload rate limit configuration from system config
     * 重新加载限流配置
     */
    public void reloadRateLimitConfig() {
        loadRateLimitConfig();
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI();

        // Check if path is rate limited
        if (!isRateLimitedPath(path)) {
            return true;
        }

        // Get client IP
        String ip = SimplePicUtils.getClientIpAddress(request);

        // Check rate limit
        if (!rateLimiter.tryAcquire(ip, maxRequests, timeWindow)) {
            logger.warn("Rate limit exceeded for IP: {}", ip);
            response.setStatus(429);
            response.getWriter().write("{\"error\":\"" + ErrorMessages.getZh("rate_limit_exceeded") + "\",\"error_en\":\"" + ErrorMessages.getEn("rate_limit_exceeded") + "\"}");
            return false;
        }

        return true;
    }

    private boolean isRateLimitedPath(String path) {
        for (String limitedPath : RATE_LIMITED_PATHS) {
            if (path.startsWith(limitedPath)) {
                return true;
            }
        }
        return false;
    }
}