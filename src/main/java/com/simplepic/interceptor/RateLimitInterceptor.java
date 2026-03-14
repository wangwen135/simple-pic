package com.simplepic.interceptor;

import com.simplepic.security.RateLimiter;
import com.simplepic.util.ErrorMessages;
import com.simplepic.util.SimplePicUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

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

    // Paths that are rate limited
    private static final String[] RATE_LIMITED_PATHS = {
            "/api/image/upload",
            "/api/admin/"
    };

    private int maxRequests = 100;
    private int timeWindow = 60;

    public void setRateLimitConfig(int maxRequests, int timeWindow) {
        this.maxRequests = maxRequests;
        this.timeWindow = timeWindow;
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