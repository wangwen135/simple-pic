/**
 * Admin Components Loader
 * Loads and initializes shared admin layout components
 */

const AdminComponents = {
    // Theme icons
    sunIcon: '<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 3v1m0 16v1m9-9h-1M4 12H3m15.364 6.364l-.707-.707M6.343 6.343l-.707-.707m12.728 0l-.707.707M6.343 17.657l-.707.707M16 12a4 4 0 11-8 0 4 4 0 018 0z"></path>',
    moonIcon: '<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20.354 15.354A9 9 0 018.646 3.646 9.003 9.003 0 0012 21a9.003 9.003 0 008.354-5.646z"></path>',

    /**
     * Load a component from URL and insert it into target element
     */
    async loadComponent(url, targetId) {
        try {
            const response = await fetch(url);
            const html = await response.text();
            const target = document.getElementById(targetId);
            if (target) {
                target.innerHTML = html;
                return true;
            } else {
                console.error(`Target element #${targetId} not found`);
                return false;
            }
        } catch (error) {
            console.error(`Failed to load component from ${url}:`, error);
            return false;
        }
    },

    /**
     * Initialize all admin layout components
     */
    async init(menuName = null) {
        // Show loading state
        this.showLoading();

        // Load all components in parallel
        const [headerLoaded, sidebarLoaded, modalLoaded] = await Promise.all([
            this.loadComponent('/admin/components/header.html', 'header-container'),
            this.loadComponent('/admin/components/sidebar.html', 'sidebar-container'),
            this.loadComponent('/admin/components/change-password-modal.html', 'modal-container')
        ]);

        // Check if all components loaded successfully
        if (!headerLoaded || !sidebarLoaded || !modalLoaded) {
            this.hideLoading();
            console.error('Failed to load some components');
            return false;
        }

        // Initialize header events
        this.initHeaderEvents();

        // Set active menu item
        if (menuName) {
            this.setActiveMenu(menuName);
        }

        // Apply i18n to loaded components
        if (typeof i18n !== 'undefined') {
            i18n.applyLanguage();
        }

        // Setup ESC key to close change password modal
        this.setupEscKeyHandler();

        // Hide loading state
        this.hideLoading();

        return true;
    },

    /**
     * Initialize header event handlers
     */
    initHeaderEvents() {
        // Load user info
        this.loadUserInfo();

        // Theme toggle
        const themeBtn = document.getElementById('themeBtn');
        if (themeBtn) {
            themeBtn.addEventListener('click', () => {
                if (typeof i18n !== 'undefined') {
                    i18n.toggleTheme();
                    this.updateThemeIcon();
                }
            });
            this.updateThemeIcon();
        }

        // Language toggle
        const langBtn = document.getElementById('langBtn');
        if (langBtn) {
            langBtn.addEventListener('click', () => {
                if (typeof i18n !== 'undefined') {
                    i18n.toggleLanguage();
                    this.updateLangText();
                }
            });
            this.updateLangText();
        }

        // Click outside to close user menu
        document.addEventListener('click', (e) => {
            const userInfo = document.getElementById('user-info');
            const menuContainer = document.getElementById('user-menu-container');
            if (userInfo && !userInfo.contains(e.target) && menuContainer && !menuContainer.contains(e.target)) {
                menuContainer.classList.add('hidden');
                menuContainer.innerHTML = '';
            }
        });
    },

    /**
     * Load user info and setup user menu
     */
    async loadUserInfo() {
        try {
            const response = await fetch('/api/auth/me');
            const data = await response.json();
            if (data.success && data.user) {
                const usernameEl = document.getElementById('username');
                if (usernameEl) {
                    usernameEl.textContent = data.user.username;
                }
                const userInfo = document.getElementById('user-info');
                if (userInfo) {
                    userInfo.addEventListener('click', this.toggleUserMenu);
                    userInfo.style.cursor = 'pointer';
                }
            }
        } catch (error) {
            console.error('Failed to load user info:', error);
        }
    },

    /**
     * Toggle user dropdown menu
     */
    toggleUserMenu() {
        const userInfo = document.getElementById('user-info');
        const menuContainer = document.getElementById('user-menu-container');

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
            if (typeof i18n !== 'undefined') {
                i18n.applyLanguage();
            }
        } else {
            menuContainer.classList.add('hidden');
            menuContainer.innerHTML = '';
        }
    },

    /**
     * Set active menu item in sidebar
     */
    setActiveMenu(menuName) {
        // Remove active class from all menu items
        document.querySelectorAll('.sidebar-link').forEach(link => {
            link.classList.remove('active');
            link.classList.remove('text-white');
            link.classList.add('text-primary');
        });

        // Add active class to current menu item
        const activeLink = document.querySelector(`.sidebar-link[data-menu="${menuName}"]`);
        if (activeLink) {
            activeLink.classList.add('active');
            activeLink.classList.remove('text-primary');
            activeLink.classList.add('text-white');
        }
    },

    /**
     * Update theme icon based on current theme
     */
    updateThemeIcon() {
        const themeIcon = document.getElementById('themeIcon');
        if (themeIcon && typeof i18n !== 'undefined') {
            const theme = i18n.getTheme();
            themeIcon.innerHTML = theme === 'light' ? this.sunIcon : this.moonIcon;
        }
    },

    /**
     * Update language text based on current language
     */
    updateLangText() {
        const langText = document.getElementById('langText');
        if (langText && typeof i18n !== 'undefined') {
            const lang = i18n.getLanguage();
            langText.textContent = lang === 'zh' ? '中文' : 'EN';
        }
    },

    /**
     * Show loading state
     */
    showLoading() {
        // Create loading overlay if it doesn't exist
        let loadingOverlay = document.getElementById('admin-loading-overlay');
        if (!loadingOverlay) {
            loadingOverlay = document.createElement('div');
            loadingOverlay.id = 'admin-loading-overlay';
            loadingOverlay.className = 'fixed inset-0 bg-slate-900/50 flex items-center justify-center z-[9999]';
            loadingOverlay.innerHTML = `
                <div class="glass rounded-xl p-6 flex items-center space-x-3">
                    <svg class="animate-spin h-5 w-5 text-blue-500" fill="none" viewBox="0 0 24 24">
                        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                    </svg>
                    <span class="text-primary">Loading...</span>
                </div>
            `;
            document.body.appendChild(loadingOverlay);
        }
        loadingOverlay.classList.remove('hidden');
    },

    /**
     * Hide loading state
     */
    hideLoading() {
        const loadingOverlay = document.getElementById('admin-loading-overlay');
        if (loadingOverlay) {
            loadingOverlay.classList.add('hidden');
        }
    },

    /**
     * Setup ESC key handler for modal
     */
    setupEscKeyHandler() {
        // Remove existing handler if any
        const existingHandler = this._escKeyHandler;
        if (existingHandler) {
            document.removeEventListener('keydown', existingHandler);
        }

        // Add new handler
        this._escKeyHandler = (e) => {
            if (e.key === 'Escape') {
                const modal = document.getElementById('change-password-modal');
                if (modal && !modal.classList.contains('hidden')) {
                    this.closeChangePasswordModal();
                }
            }
        };
        document.addEventListener('keydown', this._escKeyHandler);
    },

    /**
     * Close change password modal
     */
    closeChangePasswordModal() {
        const modal = document.getElementById('change-password-modal');
        if (modal) {
            modal.classList.add('hidden');
        }
        const menuContainer = document.getElementById('user-menu-container');
        if (menuContainer) {
            menuContainer.classList.add('hidden');
            menuContainer.innerHTML = '';
        }
    }
};

// Global functions for onclick handlers
window.toggleUserMenu = function() {
    AdminComponents.toggleUserMenu();
};

window.showChangePasswordModal = function() {
    const modal = document.getElementById('change-password-modal');
    const menuContainer = document.getElementById('user-menu-container');
    if (modal) {
        modal.classList.remove('hidden');
    }
    if (menuContainer) {
        menuContainer.classList.add('hidden');
        menuContainer.innerHTML = '';
    }
    // Clear form fields
    const currentPwd = document.getElementById('current-password');
    const newPwd = document.getElementById('new-password');
    const confirmPwd = document.getElementById('confirm-password');
    if (currentPwd) currentPwd.value = '';
    if (newPwd) newPwd.value = '';
    if (confirmPwd) confirmPwd.value = '';
};

window.closeChangePasswordModal = function() {
    AdminComponents.closeChangePasswordModal();
};

window.closeChangePasswordModalOnClick = function(event) {
    if (event.target.id === 'change-password-modal') {
        AdminComponents.closeChangePasswordModal();
    }
};

window.submitChangePassword = async function() {
    const currentPassword = document.getElementById('current-password')?.value;
    const newPassword = document.getElementById('new-password')?.value;
    const confirmPassword = document.getElementById('confirm-password')?.value;

    // Validation
    if (!currentPassword || !newPassword || !confirmPassword) {
        if (typeof Toast !== 'undefined' && typeof i18n !== 'undefined') {
            Toast.error(i18n.t('please_fill_all_fields'));
        }
        return;
    }

    if (newPassword !== confirmPassword) {
        if (typeof Toast !== 'undefined' && typeof i18n !== 'undefined') {
            Toast.error(i18n.t('passwords_not_match'));
        }
        return;
    }

    if (newPassword.length < 6) {
        if (typeof Toast !== 'undefined' && typeof i18n !== 'undefined') {
            Toast.error(i18n.t('password_too_short'));
        }
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
            if (typeof Toast !== 'undefined' && typeof i18n !== 'undefined') {
                Toast.success(i18n.t('password_changed'));
            }
            AdminComponents.closeChangePasswordModal();
        } else {
            const errorMsg = result.error || (typeof i18n !== 'undefined' ? i18n.t('password_change_failed') : 'Failed');
            if (typeof Toast !== 'undefined') {
                Toast.error(errorMsg);
            }
        }
    } catch (error) {
        if (typeof Toast !== 'undefined') {
            const errorMsg = typeof i18n !== 'undefined' ? i18n.t('password_change_failed') : 'Failed';
            Toast.error(errorMsg + ': ' + error.message);
        }
    }
};

window.logout = async function() {
    try {
        await fetch('/api/auth/logout', { method: 'POST' });
        window.location.href = '/login.html';
    } catch (error) {
        console.error('Logout failed:', error);
    }
};

// Auto-initialize on DOM ready
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', () => {
        // Auto-init if page has admin containers
        if (document.getElementById('header-container')) {
            AdminComponents.init();
        }
    });
} else {
    // Auto-init if page has admin containers
    if (document.getElementById('header-container')) {
        AdminComponents.init();
    }
}
