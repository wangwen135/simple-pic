/**
 * Simple-Pic Common JavaScript Library
 * 通用JavaScript库 - 用于所有页面的共享功能
 */

// Theme icons SVG strings
// 主题图标SVG字符串
const THEME_ICONS = {
    sun: '<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 3v1m0 16v1m9-9h-1M4 12H3m15.364 6.364l-.707-.707M6.343 6.343l-.707-.707m12.728 0l-.707.707M6.343 17.657l-.707.707M16 12a4 4 0 11-8 0 4 4 0 018 0z"></path>',
    moon: '<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20.354 15.354A9 9 0 018.646 3.646 9.003 9.003 0 0012 21a9.003 9.003 0 008.354-5.646z"></path>'
};

/**
 * Safe localStorage wrapper with error handling
 * 安全的localStorage包装器，带错误处理
 * Handles cases where localStorage is not available (e.g., private browsing mode)
 * 处理localStorage不可用的情况（如隐私浏览模式）
 */
const safeStorage = {
    get: function(key) {
        try {
            return localStorage.getItem(key);
        } catch (e) {
            console.warn('localStorage not available:', e);
            return null;
        }
    },
    set: function(key, value) {
        try {
            localStorage.setItem(key, value);
            return true;
        } catch (e) {
            console.warn('localStorage not available:', e);
            return false;
        }
    },
    remove: function(key) {
        try {
            localStorage.removeItem(key);
            return true;
        } catch (e) {
            console.warn('localStorage not available:', e);
            return false;
        }
    },
    clear: function() {
        try {
            localStorage.clear();
            return true;
        } catch (e) {
            console.warn('localStorage not available:', e);
            return false;
        }
    }
};

/**
 * Format bytes to human-readable format
 * 将字节数格式化为人类可读的格式
 * @param {number} bytes - Bytes to format
 * @returns {string} Formatted string (e.g., "1.5 MB")
 */
function formatBytes(bytes) {
    if (bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
}

/**
 * Format file size string to bytes
 * 将文件大小字符串转换为字节数
 * @param {string} sizeStr - Size string (e.g., "10MB", "1GB")
 * @returns {number} Size in bytes, or 0 if invalid format
 */
function parseFileSize(sizeStr) {
    if (!sizeStr) return 0;
    sizeStr = sizeStr.trim().toUpperCase();
    const match = sizeStr.match(/^(\d+(?:\.\d+)?)\s*(B|KB|MB|GB|TB)?$/);
    if (!match) return 0;

    const value = parseFloat(match[1]);
    const unit = match[2] || 'MB';

    const multipliers = {
        'B': 1,
        'KB': 1024,
        'MB': 1024 * 1024,
        'GB': 1024 * 1024 * 1024,
        'TB': 1024 * 1024 * 1024 * 1024
    };

    return value * (multipliers[unit] || 1);
}

/**
 * Initialize theme toggle functionality
 * 初始化主题切换功能
 * @param {string} btnId - Theme button element ID
 * @param {string} iconId - Theme icon element ID
 */
function initThemeToggle(btnId = 'themeBtn', iconId = 'themeIcon') {
    const btn = document.getElementById(btnId);
    const icon = document.getElementById(iconId);

    if (btn) {
        btn.addEventListener('click', () => {
            i18n.toggleTheme();
            updateThemeIcon(iconId);
        });
    }

    updateThemeIcon(iconId);
}

/**
 * Update theme icon based on current theme
 * 根据当前主题更新主题图标
 * @param {string} iconId - Theme icon element ID
 */
function updateThemeIcon(iconId = 'themeIcon') {
    const icon = document.getElementById(iconId);
    if (icon) {
        const theme = i18n.getTheme();
        icon.innerHTML = theme === 'light' ? THEME_ICONS.sun : THEME_ICONS.moon;
    }
}

/**
 * Initialize language toggle functionality
 * 初始化语言切换功能
 * @param {string} btnId - Language button element ID
 * @param {string} textId - Language text element ID
 */
function initLanguageToggle(btnId = 'langBtn', textId = 'langText') {
    const btn = document.getElementById(btnId);
    const text = document.getElementById(textId);

    if (btn) {
        btn.addEventListener('click', () => {
            i18n.toggleLanguage();
            updateLanguageText(textId);
        });
    }

    updateLanguageText(textId);
}

/**
 * Update language text based on current language
 * 根据当前语言更新语言文本
 * @param {string} textId - Language text element ID
 */
function updateLanguageText(textId = 'langText') {
    const text = document.getElementById(textId);
    if (text) {
        const lang = i18n.getLanguage();
        text.textContent = lang === 'zh' ? '中文' : 'EN';
    }
}

/**
 * Load user information and initialize user menu
 * 加载用户信息并初始化用户菜单
 * @param {string} usernameId - Username display element ID
 * @param {string} userInfoId - User info container element ID
 * @param {string} menuContainerId - User menu container element ID
 */
async function loadUserInfo(usernameId = 'username', userInfoId = 'user-info', menuContainerId = 'user-menu-container') {
    try {
        const response = await fetch('/api/auth/me');
        const data = await response.json();
        if (data.success && data.user) {
            const usernameEl = document.getElementById(usernameId);
            const userInfoEl = document.getElementById(userInfoId);

            if (usernameEl) {
                usernameEl.textContent = data.user.username;
            }

            if (userInfoEl) {
                userInfoEl.addEventListener('click', () => toggleUserMenu(userInfoId, menuContainerId));
                userInfoEl.style.cursor = 'pointer';
            }
        }
    } catch (error) {
        console.error('Failed to load user info:', error);
    }
}

/**
 * Toggle user menu visibility
 * 切换用户菜单的显示状态
 * @param {string} userInfoId - User info container element ID
 * @param {string} menuContainerId - User menu container element ID
 */
function toggleUserMenu(userInfoId = 'user-info', menuContainerId = 'user-menu-container') {
    const userInfo = document.getElementById(userInfoId);
    const menuContainer = document.getElementById(menuContainerId);

    if (!userInfo || !menuContainer) return;

    if (menuContainer.classList.contains('hidden')) {
        // Show menu
        menuContainer.innerHTML = `
            <div class="glass rounded-lg shadow-lg w-48 py-2">
                <button class="w-full text-left px-4 py-2 text-sm hover:bg-slate-700/50 text-primary flex items-center space-x-2" onclick="showChangePasswordModal()">
                    <svg class="w-4 h-4 text-muted" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 7a2 2 0 012 2m4 0a6 6 0 01-7.743 5.743L11 17H9v2H7v2H4a1 1 0 01-1-1v-2.586a1 1 0 01.293-.707l5.964-5.964A6 6 0 1121 9z"></path>
                    </svg>
                    <span data-i18n="change_password">Change Password</span>
                </button>
                <button class="w-full text-left px-4 py-2 text-sm hover:bg-slate-700/50 text-primary flex items-center space-x-2" onclick="logout()">
                    <svg class="w-4 h-4 text-muted" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1"></path>
                    </svg>
                    <span data-i18n="logout">Logout</span>
                </button>
            </div>
        `;

        // Position menu
        const rect = userInfo.getBoundingClientRect();
        menuContainer.style.top = (rect.bottom + 5) + 'px';
        menuContainer.style.right = (window.innerWidth - rect.right - 20) + 'px';

        menuContainer.classList.remove('hidden');
        if (typeof i18n !== 'undefined' && i18n.applyLanguage) {
            i18n.applyLanguage();
        }
    } else {
        menuContainer.classList.add('hidden');
        menuContainer.innerHTML = '';
    }
}

/**
 * Initialize user menu click-outside handler
 * 初始化用户菜单的点击外部关闭处理器
 * @param {string} userInfoId - User info container element ID
 * @param {string} menuContainerId - User menu container element ID
 */
function initUserMenuCloseHandler(userInfoId = 'user-info', menuContainerId = 'user-menu-container') {
    document.addEventListener('click', (e) => {
        const userInfo = document.getElementById(userInfoId);
        const menuContainer = document.getElementById(menuContainerId);
        if (userInfo && menuContainer && !userInfo.contains(e.target) && !menuContainer.contains(e.target)) {
            menuContainer.classList.add('hidden');
            menuContainer.innerHTML = '';
        }
    });
}

/**
 * Show change password modal
 * 显示修改密码模态框
 * @param {string} modalId - Modal element ID
 */
function showChangePasswordModal(modalId = 'change-password-modal') {
    const modal = document.getElementById(modalId);
    if (modal) {
        modal.classList.remove('hidden');
        // Close user menu if open
        const menuContainer = document.getElementById('user-menu-container');
        if (menuContainer) {
            menuContainer.classList.add('hidden');
            menuContainer.innerHTML = '';
        }
        // Clear form fields
        const currentPassword = document.getElementById('current-password');
        const newPassword = document.getElementById('new-password');
        const confirmPassword = document.getElementById('confirm-password');
        if (currentPassword) currentPassword.value = '';
        if (newPassword) newPassword.value = '';
        if (confirmPassword) confirmPassword.value = '';
    }
}

/**
 * Close change password modal
 * 关闭修改密码模态框
 * @param {string} modalId - Modal element ID
 */
function closeChangePasswordModal(modalId = 'change-password-modal') {
    const modal = document.getElementById(modalId);
    if (modal) {
        modal.classList.add('hidden');
    }
}

/**
 * Submit change password request
 * 提交修改密码请求
 */
async function submitChangePassword() {
    const currentPassword = document.getElementById('current-password').value;
    const newPassword = document.getElementById('new-password').value;
    const confirmPassword = document.getElementById('confirm-password').value;

    // Validation
    if (!currentPassword || !newPassword || !confirmPassword) {
        Toast.error(typeof i18n !== 'undefined' ? i18n.t('please_fill_all_fields') : 'Please fill all fields');
        return;
    }

    if (newPassword !== confirmPassword) {
        Toast.error(typeof i18n !== 'undefined' ? i18n.t('passwords_not_match') : 'Passwords do not match');
        return;
    }

    if (newPassword.length < 6) {
        Toast.error(typeof i18n !== 'undefined' ? i18n.t('password_too_short') : 'Password is too short');
        return;
    }

    try {
        const response = await fetch('/api/auth/change-password', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                currentPassword: currentPassword,
                newPassword: newPassword
            })
        });

        const result = await response.json();

        if (result.success) {
            Toast.success(typeof i18n !== 'undefined' ? i18n.t('password_changed') : 'Password changed successfully');
            closeChangePasswordModal();
        } else {
            Toast.error(result.error || (typeof i18n !== 'undefined' ? i18n.t('password_change_failed') : 'Failed to change password'));
        }
    } catch (error) {
        Toast.error((typeof i18n !== 'undefined' ? i18n.t('password_change_failed') : 'Failed to change password') + ': ' + error.message);
    }
}

/**
 * Logout and redirect to login page
 * 登出并重定向到登录页面
 */
async function logout() {
    try {
        await fetch('/api/auth/logout', { method: 'POST' });
        window.location.href = '/login.html';
    } catch (error) {
        console.error('Logout failed:', error);
        // Still redirect even if API call fails
        window.location.href = '/login.html';
    }
}

/**
 * Initialize all common functionality
 * 初始化所有通用功能
 * Call this in DOMContentLoaded event
 */
function initCommon() {
    updateThemeIcon();
    updateLanguageText();
    initUserMenuCloseHandler();
}

// Export functions for global access
// 导出函数供全局访问
window.safeStorage = safeStorage;
window.formatBytes = formatBytes;
window.parseFileSize = parseFileSize;
window.initThemeToggle = initThemeToggle;
window.updateThemeIcon = updateThemeIcon;
window.initLanguageToggle = initLanguageToggle;
window.updateLanguageText = updateLanguageText;
window.loadUserInfo = loadUserInfo;
window.toggleUserMenu = toggleUserMenu;
window.initUserMenuCloseHandler = initUserMenuCloseHandler;
window.showChangePasswordModal = showChangePasswordModal;
window.closeChangePasswordModal = closeChangePasswordModal;
window.submitChangePassword = submitChangePassword;
window.logout = logout;
window.initCommon = initCommon;
window.THEME_ICONS = THEME_ICONS;
