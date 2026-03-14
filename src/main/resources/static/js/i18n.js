/**
 * i18n and Theme Utility
 * Internationalization and theme management
 */

const i18n = {
    // Theme management
    theme: {
        light: 'light',
        dark: 'dark'
    },

    // Language management
    languages: {
        zh: 'zh',
        en: 'en'
    },

    // Translation dictionaries
    translations: {
        zh: {
            // Common
            app_title: "Simple-Pic",
            app_description: "Simple local image hosting",
            upload: "上传",
            login: "登录",
            logout: "登出",
            admin_panel: "管理后台",
            theme: "主题",
            language: "语言",
            light: "亮色",
            dark: "暗色",
            settings: "设置",

            // Upload page
            drop_images_here: "拖拽图片到这里",
            or_click_to_select: "或点击选择 • 粘贴剪贴板 (Ctrl+V)",
            link_format: "链接格式",
            markdown: "Markdown",
            direct_link: "直接链接",
            html: "HTML",
            bbcode: "BBCode",
            recent_uploads: "最近上传",
            clear_history: "清空历史",
            no_uploads_yet: "暂无上传",
            image_uploaded: "图片上传成功，链接已复制！",
            link_copied: "链接已复制！",
            history_cleared: "历史已清空",
            upload_failed: "上传失败",
            please_select_images: "请仅选择图片文件",
            not_logged_in: "未登录",
            storage: "存储空间",

            // Login page
            username: "用户名",
            password: "密码",
            remember_me: "记住我",
            sign_in: "登录",
            upload_without_login: "匿名上传",
            entering_username: "请输入用户名",
            entering_password: "请输入密码",
            signing_in: "登录中...",
            invalid_credentials: "用户名或密码无效",
            network_error: "网络错误，请重试",

            // Admin - Dashboard
            dashboard: "仪表盘",
            total_images: "总图片数",
            total_used: "已用空间",
            total_space: "总空间",
            active_sessions: "活跃会话",
            storage_stats: "存储统计",

            // Admin - Storage
            storage_management: "存储管理",
            storage_name: "存储名称",
            storage_path: "存储路径",
            max_size: "最大容量",
            domain: "域名",
            used_size: "已用大小",
            free_size: "可用大小",
            image_count: "图片数量",
            usage: "使用率",
            create_storage: "创建存储空间",
            edit_storage: "编辑存储空间",
            delete_storage: "删除存储空间",
            allow_anonymous: "允许匿名上传",

            // Admin - Users
            user_management: "用户管理",
            create_user: "创建用户",
            edit_user: "编辑用户",
            delete_user: "删除用户",
            role: "角色",
            admin: "管理员",
            user: "普通用户",
            assigned_storage: "分配存储空间",
            password_optional: "密码（留空不修改）",

            // Admin - System
            system_settings: "系统设置",
            system_name: "系统名称",
            system_description: "系统描述",
            anonymous_upload_enabled: "启用匿名上传",
            watermark_enabled: "启用水印",
            watermark_type: "水印类型",
            watermark_content: "水印内容",
            watermark_position: "水印位置",
            watermark_opacity: "水印透明度",
            security_settings: "安全设置",

            // Admin - API Keys
            api_key_management: "API 密钥管理",
            generate_api_key: "生成 API 密钥",
            delete_api_key: "删除 API 密钥",
            storage_space_for_key: "存储空间",
            api_key_generated: "API 密钥已生成",
            api_key_deleted: "API 密钥已删除",

            // Admin - Images
            image_management: "图片管理",
            select_storage: "选择存储空间",
            view_images: "查看图片",
            delete_image: "删除图片",

            // Common UI
            save: "保存",
            cancel: "取消",
            delete: "删除",
            confirm: "确认",
            confirm_delete: "确认删除？",
            success: "成功",
            failed: "失败",
            loading: "加载中...",
            no_data: "暂无数据",

            // Anonymous upload
            anonymous_upload_not_allowed: "匿名上传未启用",
            no_storage_for_anonymous: "没有存储空间允许匿名上传",
            select_storage_for_upload: "请选择上传存储空间",

            // Change password
            change_password: "修改密码",
            current_password: "当前密码",
            new_password: "新密码",
            confirm_password: "确认密码",
            please_fill_all_fields: "请填写所有字段",
            passwords_not_match: "两次输入的密码不一致",
            password_too_short: "密码长度至少6位",
            password_changed: "密码修改成功",
            password_change_failed: "密码修改失败"
        },
        en: {
            // Common
            app_title: "Simple-Pic",
            app_description: "Simple local image hosting",
            upload: "Upload",
            login: "Login",
            logout: "Logout",
            admin_panel: "Admin Panel",
            theme: "Theme",
            language: "Language",
            light: "Light",
            dark: "Dark",
            settings: "Settings",

            // Upload page
            drop_images_here: "Drop images here",
            or_click_to_select: "or click to select • Paste from clipboard (Ctrl+V)",
            link_format: "Link Format",
            markdown: "Markdown",
            direct_link: "Direct Link",
            html: "HTML",
            bbcode: "BBCode",
            recent_uploads: "Recent Uploads",
            clear_history: "Clear History",
            no_uploads_yet: "No uploads yet",
            image_uploaded: "Image uploaded and link copied!",
            link_copied: "Link copied!",
            history_cleared: "History cleared",
            upload_failed: "Upload failed",
            please_select_images: "Please select image files only",
            not_logged_in: "Not logged in",
            storage: "Storage Space",

            // Login page
            username: "Username",
            password: "Password",
            remember_me: "Remember me",
            sign_in: "Sign In",
            upload_without_login: "Upload without login",
            entering_username: "Enter your username",
            entering_password: "Enter your password",
            signing_in: "Signing in...",
            invalid_credentials: "Invalid username or password",
            network_error: "Network error. Please try again",

            // Admin - Dashboard
            dashboard: "Dashboard",
            total_images: "Total Images",
            total_used: "Total Used",
            total_space: "Total Space",
            active_sessions: "Active Sessions",
            storage_stats: "Storage Stats",

            // Admin - Storage
            storage_management: "Storage Management",
            storage_name: "Storage Name",
            storage_path: "Storage Path",
            max_size: "Max Size",
            domain: "Domain",
            used_size: "Used Size",
            free_size: "Free Size",
            image_count: "Image Count",
            usage: "Usage",
            create_storage: "Create Storage Space",
            edit_storage: "Edit Storage Space",
            delete_storage: "Delete Storage Space",
            allow_anonymous: "Allow Anonymous Upload",

            // Admin - Users
            user_management: "User Management",
            create_user: "Create User",
            edit_user: "Edit User",
            delete_user: "Delete User",
            role: "Role",
            admin: "Admin",
            user: "User",
            assigned_storage: "Assigned Storage Spaces",
            password_optional: "Password (leave empty to keep unchanged)",

            // Admin - System
            system_settings: "System Settings",
            system_name: "System Name",
            system_description: "System Description",
            anonymous_upload_enabled: "Enable Anonymous Upload",
            watermark_enabled: "Enable Watermark",
            watermark_type: "Watermark Type",
            watermark_content: "Watermark Content",
            watermark_position: "Watermark Position",
            watermark_opacity: "Watermark Opacity",

            // Admin - API Keys
            api_key_management: "API Key Management",
            generate_api_key: "Generate API Key",
            delete_api_key: "Delete API Key",
            storage_space_for_key: "Storage Space",
            api_key_generated: "API key generated",
            api_key_deleted: "API key deleted",

            // Admin - Images
            image_management: "Image Management",
            select_storage: "Select Storage Space",
            view_images: "View Images",
            delete_image: "Delete Image",

            // Common UI
            save: "Save",
            cancel: "Cancel",
            delete: "Delete",
            confirm: "Confirm",
            confirm_delete: "Confirm delete?",
            success: "Success",
            failed: "Failed",
            loading: "Loading...",
            no_data: "No data available",

            // Anonymous upload
            anonymous_upload_not_allowed: "Anonymous upload is not enabled",
            no_storage_for_anonymous: "No storage spaces allow anonymous upload",
            select_storage_for_upload: "Please select a storage space for upload",

            // Change password
            change_password: "Change Password",
            current_password: "Current Password",
            new_password: "New Password",
            confirm_password: "Confirm Password",
            please_fill_all_fields: "Please fill in all fields",
            passwords_not_match: "Passwords do not match",
            password_too_short: "Password must be at least 6 characters",
            password_changed: "Password changed successfully",
            password_change_failed: "Failed to change password"
        }
    },

    /**
     * Get translation text by key
     */
    t: function(key) {
        const lang = this.getLanguage();
        const dict = this.translations[lang] || this.translations.zh;
        return dict[key] || key;
    },

    /**
     * Get current language
     */
    getLanguage: function() {
        return localStorage.getItem('i18n-language') || 'zh';
    },

    /**
     * Set language
     */
    setLanguage: function(lang) {
        if (lang === 'zh' || lang === 'en') {
            localStorage.setItem('i18n-language', lang);
            this.applyLanguage();
        }
    },

    /**
     * Toggle language
     */
    toggleLanguage: function() {
        const current = this.getLanguage();
        const newLang = current === 'zh' ? 'en' : 'zh';
        this.setLanguage(newLang);
        return newLang;
    },

    /**
     * Apply language to all elements with data-i18n attribute
     */
    applyLanguage: function() {
        document.querySelectorAll('[data-i18n]').forEach(el => {
            const key = el.getAttribute('data-i18n');
            el.textContent = this.t(key);
        });

        // Update placeholder attributes
        document.querySelectorAll('[data-i18n-placeholder]').forEach(el => {
            const key = el.getAttribute('data-i18n-placeholder');
            el.placeholder = this.t(key);
        });
    },

    /**
     * Get current theme
     */
    getTheme: function() {
        return localStorage.getItem('theme') || 'light';
    },

    /**
     * Set theme
     */
    setTheme: function(theme) {
        if (theme === 'light' || theme === 'dark') {
            localStorage.setItem('theme', theme);
            this.applyTheme();
        }
    },

    /**
     * Toggle theme
     */
    toggleTheme: function() {
        const current = this.getTheme();
        const newTheme = current === 'light' ? 'dark' : 'light';
        this.setTheme(newTheme);
        return newTheme;
    },

    /**
     * Apply theme to document
     */
    applyTheme: function() {
        const theme = this.getTheme();
        document.documentElement.setAttribute('data-theme', theme);
    },

    /**
     * Initialize i18n and theme
     */
    init: function() {
        this.applyTheme();
        this.applyLanguage();
    }
};

// Initialize on DOM ready
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', () => i18n.init());
} else {
    i18n.init();
}