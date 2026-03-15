/**
 * Toast Notification Component
 * Provides unified toast notification functionality
 */

const Toast = {
    container: null,

    /**
     * Initialize the toast container
     */
    init() {
        if (!this.container) {
            this.container = document.createElement('div');
            this.container.id = 'toast-container';
            this.container.className = 'fixed top-4 left-1/2 transform -translate-x-1/2 z-[10001] flex flex-col gap-2';
            document.body.appendChild(this.container);
        }
    },

    /**
     * Show a toast notification
     * @param {string} message - The message to display
     * @param {string} type - The type of toast: 'success', 'error', 'warning', 'info'
     * @param {number} duration - Duration in milliseconds (default: 3000)
     */
    show(message, type = 'info', duration = 3000) {
        this.init();

        const toast = document.createElement('div');
        toast.className = this.getToastClasses(type);

        const icon = this.getIcon(type);

        toast.innerHTML = `
            <div class="flex items-center gap-3">
                ${icon}
                <span class="flex-1">${message}</span>
                <button class="close-btn hover:opacity-75 transition-opacity" onclick="this.parentElement.parentElement.remove()">
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path>
                    </svg>
                </button>
            </div>
        `;

        // Animation
        toast.style.animation = 'toastSlideIn 0.3s ease';

        this.container.appendChild(toast);

        // Auto dismiss
        if (duration > 0) {
            setTimeout(() => this.dismiss(toast), duration);
        }

        return toast;
    },

    /**
     * Get toast classes based on type
     */
    getToastClasses(type) {
        const baseClasses = 'toast px-4 py-3 rounded-lg shadow-lg min-w-[300px] max-w-md';
        const textClass = 'text-white';

        const bgClasses = {
            success: 'bg-green-600',
            error: 'bg-red-600',
            warning: 'bg-yellow-600',
            info: 'bg-blue-600'
        };

        return `${baseClasses} ${bgClasses[type] || bgClasses.info} ${textClass}`;
    },

    /**
     * Get icon SVG based on type
     */
    getIcon(type) {
        const icons = {
            success: `<svg class="w-5 h-5 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"></path>
            </svg>`,
            error: `<svg class="w-5 h-5 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path>
            </svg>`,
            warning: `<svg class="w-5 h-5 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"></path>
            </svg>`,
            info: `<svg class="w-5 h-5 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path>
            </svg>`
        };

        return icons[type] || icons.info;
    },

    /**
     * Dismiss a toast with animation
     */
    dismiss(toast) {
        if (!toast || !toast.parentElement) return;

        toast.style.animation = 'toastSlideOut 0.3s ease forwards';

        setTimeout(() => {
            if (toast.parentElement) {
                toast.remove();
            }
        }, 300);
    },

    /**
     * Show success toast
     */
    success(message, duration) {
        return this.show(message, 'success', duration);
    },

    /**
     * Show error toast
     */
    error(message, duration) {
        return this.show(message, 'error', duration);
    },

    /**
     * Show warning toast
     */
    warning(message, duration) {
        return this.show(message, 'warning', duration);
    },

    /**
     * Show info toast
     */
    info(message, duration) {
        return this.show(message, 'info', duration);
    },

    /**
     * Clear all toasts
     */
    clear() {
        if (this.container) {
            this.container.innerHTML = '';
        }
    }
};

// Add animation styles to document
const style = document.createElement('style');
style.textContent = `
    @keyframes toastSlideIn {
        from {
            transform: translateY(-100%);
            opacity: 0;
        }
        to {
            transform: translateY(0);
            opacity: 1;
        }
    }

    @keyframes toastSlideOut {
        from {
            transform: translateY(0);
            opacity: 1;
        }
        to {
            transform: translateY(-100%);
            opacity: 0;
        }
    }
`;
document.head.appendChild(style);

// Export for module use
if (typeof module !== 'undefined' && module.exports) {
    module.exports = Toast;
}