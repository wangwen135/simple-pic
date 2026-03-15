package com.simplepic.service;

import com.simplepic.model.LoginSession;
import com.simplepic.model.Role;
import com.simplepic.model.SystemConfig;
import com.simplepic.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Authentication service
 * 认证服务
 */
@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    @Autowired
    private ConfigService configService;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private LoginAttemptService loginAttemptService;

    // Session storage: token -> LoginSession
    private final Map<String, LoginSession> sessions = new ConcurrentHashMap<>();

    /**
     * Login with username and password
     */
    public String login(String username, String password) {
        return login(username, password, "", true);
    }

    /**
     * Login with username, password and IP address (for lockout feature)
     */
    public String login(String username, String password, String ipAddress) {
        return login(username, password, ipAddress, true);
    }

    /**
     * Login with username, password, IP address and remember me option
     */
    public String login(String username, String password, String ipAddress, boolean rememberMe) {
        // Check if IP is locked
        if (loginAttemptService.isLocked(ipAddress)) {
            logger.warn("Login attempt from locked IP: {}", ipAddress);
            return null;
        }

        SystemConfig config = configService.getConfig();
        if (config == null || config.getUsers() == null) {
            logger.error("Configuration not loaded");
            return null;
        }

        for (SystemConfig.User userConfig : config.getUsers()) {
            if (userConfig.getUsername().equals(username)) {
                if (passwordEncoder.matches(password, userConfig.getPassword())) {
                    // Login successful, clear failed attempts
                    loginAttemptService.loginSucceeded(ipAddress);

                    // Create session with remember me setting
                    String token = generateToken();
                    String[] storageSpaces;

                    // Admin users get all available storage spaces
                    if ("ADMIN".equals(userConfig.getRole())) {
                        List<String> allSpaces = new ArrayList<>();
                        if (config.getStorageSpaces() != null) {
                            for (SystemConfig.StorageSpace space : config.getStorageSpaces()) {
                                allSpaces.add(space.getName());
                            }
                        }
                        storageSpaces = allSpaces.toArray(new String[0]);
                    } else {
                        // Regular users get their assigned storage spaces
                        storageSpaces = userConfig.getStorageSpaces().toArray(new String[0]);
                    }

                    LoginSession session = new LoginSession(username, userConfig.getRole(), storageSpaces, rememberMe);
                    sessions.put(token, session);

                    logger.info("User {} logged in successfully from IP: {}", username, ipAddress);
                    return token;
                }
            }
        }

        // Login failed, record attempt
        loginAttemptService.loginFailed(ipAddress);
        logger.warn("Login failed for user: {} from IP: {}", username, ipAddress);
        return null;
    }

    /**
     * Check if an IP is locked out
     */
    public boolean isIPLocked(String ipAddress) {
        return loginAttemptService.isLocked(ipAddress);
    }

    /**
     * Get remaining lockout minutes for an IP
     */
    public long getRemainingLockoutMinutes(String ipAddress) {
        return loginAttemptService.getRemainingLockoutMinutes(ipAddress);
    }

    /**
     * Logout
     */
    public void logout(String token) {
        LoginSession session = sessions.remove(token);
        if (session != null) {
            logger.info("User {} logged out", session.getUsername());
        }
    }

    /**
     * Get session by token
     */
    public LoginSession getSession(String token) {
        if (token == null) {
            return null;
        }
        LoginSession session = sessions.get(token);
        if (session != null) {
            if (session.isExpired()) {
                sessions.remove(token);
                return null;
            }
            // Extend session expiry on access
            session.extendExpiry();
        }
        return session;
    }

    /**
     * Get current user by token
     */
    public User getCurrentUser(String token) {
        LoginSession session = getSession(token);
        if (session == null) {
            return null;
        }

        SystemConfig config = configService.getConfig();
        if (config == null || config.getUsers() == null) {
            return null;
        }

        for (SystemConfig.User userConfig : config.getUsers()) {
            if (userConfig.getUsername().equals(session.getUsername())) {
                User user = new User();
                user.setUsername(userConfig.getUsername());
                user.setPassword(userConfig.getPassword());
                user.setRole(Role.fromString(userConfig.getRole()));

                // Admin users get all available storage spaces
                if ("ADMIN".equals(userConfig.getRole())) {
                    List<String> allStorageSpaces = new ArrayList<>();
                    if (config.getStorageSpaces() != null) {
                        for (SystemConfig.StorageSpace space : config.getStorageSpaces()) {
                            allStorageSpaces.add(space.getName());
                        }
                    }
                    user.setStorageSpaces(allStorageSpaces.toArray(new String[0]));
                } else {
                    // Regular users get their assigned storage spaces
                    user.setStorageSpaces(userConfig.getStorageSpaces().toArray(new String[0]));
                }

                user.setCurrentStorageSpace(session.getCurrentStorageSpace());
                return user;
            }
        }

        return null;
    }

    /**
     * Check if user is admin
     */
    public boolean isAdmin(String token) {
        LoginSession session = getSession(token);
        return session != null && "ADMIN".equals(session.getRole());
    }

    /**
     * Update current storage space for user
     */
    public boolean switchStorageSpace(String token, String storageSpace) {
        LoginSession session = getSession(token);
        if (session == null) {
            return false;
        }

        // Check if user has access to this storage space
        for (String space : session.getStorageSpaces()) {
            if (space.equals(storageSpace)) {
                session.setCurrentStorageSpace(storageSpace);
                return true;
            }
        }

        return false;
    }

    /**
     * Generate cryptographically secure random token
     */
    private String generateToken() {
        return com.simplepic.security.SecureTokenGenerator.generateToken();
    }

    /**
     * Get active session count
     */
    public int getActiveSessionCount() {
        return sessions.size();
    }

    /**
     * Get all active sessions
     */
    public List<Map<String, Object>> getActiveSessions() {
        List<Map<String, Object>> sessionList = new ArrayList<>();
        for (Map.Entry<String, LoginSession> entry : sessions.entrySet()) {
            Map<String, Object> sessionInfo = new java.util.HashMap<>();
            LoginSession session = entry.getValue();
            sessionInfo.put("token", entry.getKey());
            sessionInfo.put("username", session.getUsername());
            sessionInfo.put("role", session.getRole());
            sessionInfo.put("storageSpace", session.getCurrentStorageSpace());
            sessionInfo.put("loginTime", session.getLoginTime());
            sessionInfo.put("expiryTime", session.getExpiryTime());
            sessionList.add(sessionInfo);
        }
        return sessionList;
    }
}