package com.simplepic.security;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Rate limiter for IP addresses
 * IP限流器
 */
@Component
public class RateLimiter {

    private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    private final Thread cleanupThread;

    public RateLimiter() {
        // Start cleanup thread
        cleanupThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(TimeUnit.MINUTES.toMillis(5));
                    cleanup();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        cleanupThread.setDaemon(true);
        cleanupThread.start();
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