package com.wwh.simplepic.service;

import com.wwh.simplepic.model.SystemConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Login attempt tracking service
 * 登录尝试追踪服务 - 用于IP锁定功能
 */
@Service
public class LoginAttemptService {

    private static final Logger logger = LoggerFactory.getLogger(LoginAttemptService.class);

    // 存储每个IP的失败次数
    private final Map<String, Integer> attemptsCache = new ConcurrentHashMap<>();

    // 存储每个IP的锁定时间戳
    private final Map<String, Long> lockoutCache = new ConcurrentHashMap<>();

    private final ConfigService configService;
    private final ScheduledExecutorService scheduler;

    public LoginAttemptService(ConfigService configService) {
        this.configService = configService;

        // 定期清理过期的锁定记录（每分钟执行一次）
        scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(this::cleanExpiredLockouts, 1, 1, TimeUnit.MINUTES);
    }

    /**
     * Cleanup method called when bean is destroyed
     * Bean销毁时的清理方法
     */
    @PreDestroy
    public void destroy() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * 记录登录失败
     */
    public void loginFailed(String ipAddress) {
        SystemConfig config = configService.getConfig();

        // 如果功能未启用，不做处理
        if (!config.isLoginLockoutEnabled()) {
            return;
        }

        int currentAttempts = attemptsCache.getOrDefault(ipAddress, 0) + 1;
        attemptsCache.put(ipAddress, currentAttempts);

        logger.warn("Login failed attempt {} for IP: {}", currentAttempts, ipAddress);

        // 检查是否达到最大失败次数
        int maxAttempts = config.getMaxFailedAttempts();
        if (currentAttempts >= maxAttempts) {
            lockout(ipAddress, config.getLockoutMinutes());
        }
    }

    /**
     * 记录登录成功，清除失败记录
     */
    public void loginSucceeded(String ipAddress) {
        attemptsCache.remove(ipAddress);
        lockoutCache.remove(ipAddress);
        logger.debug("Login success for IP: {}, attempts cleared", ipAddress);
    }

    /**
     * 检查IP是否被锁定
     */
    public boolean isLocked(String ipAddress) {
        SystemConfig config = configService.getConfig();

        // 如果功能未启用，始终返回false
        if (!config.isLoginLockoutEnabled()) {
            return false;
        }

        Long lockoutTime = lockoutCache.get(ipAddress);
        if (lockoutTime == null) {
            return false;
        }

        // 检查锁定是否过期
        long lockoutMinutes = config.getLockoutMinutes();
        long expireTime = lockoutTime + TimeUnit.MINUTES.toMillis(lockoutMinutes);

        if (System.currentTimeMillis() > expireTime) {
            // 锁定已过期，清除记录
            lockoutCache.remove(ipAddress);
            attemptsCache.remove(ipAddress);
            return false;
        }

        return true;
    }

    /**
     * 获取IP的剩余锁定时间（分钟）
     */
    public long getRemainingLockoutMinutes(String ipAddress) {
        Long lockoutTime = lockoutCache.get(ipAddress);
        if (lockoutTime == null) {
            return 0;
        }

        SystemConfig config = configService.getConfig();
        long lockoutMinutes = config.getLockoutMinutes();
        long expireTime = lockoutTime + TimeUnit.MINUTES.toMillis(lockoutMinutes);
        long remainingMillis = expireTime - System.currentTimeMillis();

        return Math.max(0, TimeUnit.MILLISECONDS.toMinutes(remainingMillis) + 1);
    }

    /**
     * 获取IP的当前失败次数
     */
    public int getFailedAttempts(String ipAddress) {
        return attemptsCache.getOrDefault(ipAddress, 0);
    }

    /**
     * 锁定IP
     */
    private void lockout(String ipAddress, int minutes) {
        lockoutCache.put(ipAddress, System.currentTimeMillis());
        logger.warn("IP {} has been locked out for {} minutes due to too many failed login attempts", ipAddress, minutes);
    }

    /**
     * Clean up expired lockout records
     * 清理过期的锁定记录
     * Runs periodically to remove lockout records that have passed their expiration time
     * 定期运行以移除已过期的锁定记录
     */
    private void cleanExpiredLockouts() {
        SystemConfig config = configService.getConfig();
        long lockoutMinutes = TimeUnit.MINUTES.toMillis(config.getLockoutMinutes());
        long currentTime = System.currentTimeMillis();

        lockoutCache.entrySet().removeIf(entry -> {
            if (currentTime - entry.getValue() > lockoutMinutes) {
                String ipAddress = entry.getKey();
                attemptsCache.remove(ipAddress);
                logger.debug("Removed expired lockout for IP: {}", ipAddress);
                return true;
            }
            return false;
        });
    }
}