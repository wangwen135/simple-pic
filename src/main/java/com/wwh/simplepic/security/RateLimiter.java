package com.wwh.simplepic.security;

import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * IP限流器 - 使用令牌桶算法
 */
@Component
public class RateLimiter {

    // IP地址到令牌桶的映射
    private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    // 被限流的IP记录：IP -> {rejectedCount, lastRejectedTime}
    private final Map<String, long[]> rateLimitedIps = new ConcurrentHashMap<>();

    private ScheduledExecutorService cleanupExecutor;

    /**
     * 初始化限流器
     */
    @PostConstruct
    public void init() {
        // 初始化清理任务的定时执行器
        cleanupExecutor = Executors.newScheduledThreadPool(1);
        cleanupExecutor.scheduleAtFixedRate(
                this::cleanup,
                5, 5, TimeUnit.MINUTES);
    }

    /**
     * Bean销毁时的清理
     */
    @PreDestroy
    public void destroy() {
        // 关闭执行器
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
     * 检查请求是否在限流范围内允许
     */
    public boolean tryAcquire(String ip, int maxRequests, int timeWindow) {
        TokenBucket bucket = buckets.computeIfAbsent(ip, k -> new TokenBucket(maxRequests, timeWindow));
        boolean allowed = bucket.tryConsume();
        if (!allowed) {
            rateLimitedIps.compute(ip, (key, val) -> {
                if (val == null) {
                    return new long[]{1, System.currentTimeMillis()};
                }
                val[0]++;
                val[1] = System.currentTimeMillis();
                return val;
            });
        }
        return allowed;
    }

    /**
     * 重置特定IP的限流
     */
    public void reset(String ip) {
        buckets.remove(ip);
        rateLimitedIps.remove(ip);
    }

    /**
     * 获取当前被限流的IP列表
     */
    public List<Map<String, Object>> getRateLimitedIps() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, long[]> entry : rateLimitedIps.entrySet()) {
            Map<String, Object> info = new HashMap<>();
            info.put("ip", entry.getKey());
            info.put("rejectedCount", entry.getValue()[0]);
            info.put("lastRejectedAt", entry.getValue()[1]);
            result.add(info);
        }
        return result;
    }

    /**
     * 清理过期的令牌桶和限流记录
     */
    private void cleanup() {
        long now = System.currentTimeMillis();
        buckets.entrySet().removeIf(entry -> {
            if (entry.getValue().isExpired(now)) {
                rateLimitedIps.remove(entry.getKey());
                return true;
            }
            return false;
        });
    }

    /**
     * 令牌桶实现 - 每个IP有自己的令牌桶，随时间自动填充
     */
    private static class TokenBucket {
        private final int maxTokens;
        /** 内部填充间隔（毫秒，由构造函数中的秒数转换） */
        private final long refillInterval;
        private int tokens;
        private long lastRefillTime;

        /**
         * 创建新的令牌桶
         */
        public TokenBucket(int maxTokens, long refillInterval) {
            this.maxTokens = maxTokens;
            // 秒转毫秒，内部时间计算使用 System.currentTimeMillis()
            this.refillInterval = refillInterval * 1000L;
            this.tokens = maxTokens;
            this.lastRefillTime = System.currentTimeMillis();
        }

        /**
         * 尝试消费一个令牌
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
         * 检查令牌桶是否过期（用于清理）
         */
        public boolean isExpired(long now) {
            return (now - lastRefillTime) > refillInterval * 2;
        }
    }
}