package com.simplepic.security;

import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Rate limiter for IP addresses
 * IP限流器
 */
@Component
public class RateLimiter {

    private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    private ScheduledExecutorService cleanupExecutor;

    @PostConstruct
    public void init() {
        // Initialize scheduled executor for cleanup
        cleanupExecutor = Executors.newScheduledThreadPool(1);
        cleanupExecutor.scheduleAtFixedRate(
                this::cleanup,
                5, 5, TimeUnit.MINUTES);
    }

    @PreDestroy
    public void destroy() {
        // Shutdown executor on bean destruction
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
     * Check if request is allowed
     */
    public boolean tryAcquire(String ip, int maxRequests, int timeWindow) {
        TokenBucket bucket = buckets.computeIfAbsent(ip, k -> new TokenBucket(maxRequests, timeWindow));
        return bucket.tryConsume();
    }

    /**
     * Reset rate limit for IP
     */
    public void reset(String ip) {
        buckets.remove(ip);
    }

    /**
     * Clean up expired buckets
     */
    private void cleanup() {
        long now = System.currentTimeMillis();
        buckets.entrySet().removeIf(entry -> entry.getValue().isExpired(now));
    }

    /**
     * Token bucket implementation
     */
    private static class TokenBucket {
        private final int maxTokens;
        private final long refillInterval;
        private int tokens;
        private long lastRefillTime;

        public TokenBucket(int maxTokens, long refillInterval) {
            this.maxTokens = maxTokens;
            this.refillInterval = refillInterval * 1000L; // Convert to milliseconds
            this.tokens = maxTokens;
            this.lastRefillTime = System.currentTimeMillis();
        }

        public synchronized boolean tryConsume() {
            refill();
            if (tokens > 0) {
                tokens--;
                return true;
            }
            return false;
        }

        private void refill() {
            long now = System.currentTimeMillis();
            long elapsed = now - lastRefillTime;

            if (elapsed >= refillInterval) {
                int refillTokens = (int) (elapsed / refillInterval) * maxTokens;
                tokens = Math.min(tokens + refillTokens, maxTokens);
                lastRefillTime = now;
            }
        }

        public boolean isExpired(long now) {
            return (now - lastRefillTime) > refillInterval * 2;
        }
    }
}