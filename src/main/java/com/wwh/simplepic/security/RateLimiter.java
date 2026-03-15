package com.wwh.simplepic.security;

import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Rate limiter for IP addresses using token bucket algorithm
 * IP限流器 - 使用令牌桶算法
 */
@Component
public class RateLimiter {

    // Map of IP addresses to their token buckets
    // IP地址到令牌桶的映射
    private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    private ScheduledExecutorService cleanupExecutor;

    /**
     * Initialize the rate limiter
     * 初始化限流器
     */
    @PostConstruct
    public void init() {
        // Initialize scheduled executor for cleanup (初始化清理任务的定时执行器)
        cleanupExecutor = Executors.newScheduledThreadPool(1);
        cleanupExecutor.scheduleAtFixedRate(
                this::cleanup,
                5, 5, TimeUnit.MINUTES);
    }

    /**
     * Cleanup on bean destruction
     * Bean销毁时的清理
     */
    @PreDestroy
    public void destroy() {
        // Shutdown executor on bean destruction (关闭执行器)
        if (cleanupExecutor != null) {
            cleanupExecutor.shutdown();
            try {
                if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    cleanupExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                cleanupExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Check if request is allowed under rate limit
     * 检查请求是否在限流范围内允许
     *
     * @param ip          client IP address
     * @param maxRequests maximum requests allowed in time window
     * @param timeWindow  time window in seconds
     * @return true if request is allowed, false if rate limit exceeded
     */
    public boolean tryAcquire(String ip, int maxRequests, int timeWindow) {
        TokenBucket bucket = buckets.computeIfAbsent(ip, k -> new TokenBucket(maxRequests, timeWindow));
        return bucket.tryConsume();
    }

    /**
     * Reset rate limit for specific IP
     * 重置特定IP的限流
     *
     * @param ip client IP address
     */
    public void reset(String ip) {
        buckets.remove(ip);
    }

    /**
     * Clean up expired buckets
     * 清理过期的令牌桶
     */
    private void cleanup() {
        long now = System.currentTimeMillis();
        buckets.entrySet().removeIf(entry -> entry.getValue().isExpired(now));
    }

    /**
     * Token bucket implementation
     * 令牌桶实现
     * Each IP has its own token bucket that refills over time
     * 每个IP都有自己的令牌桶，随时间自动填充
     */
    private static class TokenBucket {
        private final int maxTokens;
        private final long refillInterval;
        private int tokens;
        private long lastRefillTime;

        /**
         * Create a new token bucket
         * 创建新的令牌桶
         *
         * @param maxTokens      maximum tokens the bucket can hold
         * @param refillInterval time in seconds between refills
         */
        public TokenBucket(int maxTokens, long refillInterval) {
            this.maxTokens = maxTokens;
            this.refillInterval = refillInterval * 1000L; // Convert to milliseconds (转换为毫秒)
            this.tokens = maxTokens;
            this.lastRefillTime = System.currentTimeMillis();
        }

        /**
         * Try to consume a token
         * 尝试消费一个令牌
         *
         * @return true if token was consumed, false if bucket is empty
         */
        public synchronized boolean tryConsume() {
            refill();
            if (tokens > 0) {
                tokens--;
                return true;
            }
            return false;
        }

        /**
         * Refill tokens based on elapsed time
         * 根据经过的时间填充令牌
         */
        private void refill() {
            long now = System.currentTimeMillis();
            long elapsed = now - lastRefillTime;

            if (elapsed >= refillInterval) {
                int refillTokens = (int) (elapsed / refillInterval) * maxTokens;
                tokens = Math.min(tokens + refillTokens, maxTokens);
                lastRefillTime = now;
            }
        }

        /**
         * Check if bucket is expired (for cleanup)
         * 检查令牌桶是否过期（用于清理）
         *
         * @param now current time in milliseconds
         * @return true if bucket is expired
         */
        public boolean isExpired(long now) {
            return (now - lastRefillTime) > refillInterval * 2;
        }
    }
}