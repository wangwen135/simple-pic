/**
 * Confirmation Dialog Component
 * Provides unified confirmation dialog functionality
 */

const Confirm = {
    container: null,
    activeConfirm: null,

    /**
     * Initialize the confirm container
     */
    init() {
        if (!this.container) {
            this.container = document.createElement('div');
            this.container.id = 'confirm-container';
            this.container.className = 'fixed inset-0 z-50 flex items-center justify-center';
            document.body.appendChild(this.container);
        }
    },

    /**
     * Show a confirmation dialog
     * @param {Object} options - Configuration options
     * @param {string} options.title - Dialog title
     * @param {string} options.message - Dialog message
     * @param {string} options.confirmText - Confirm button text (default: 'Confirm')
     * @param {string} options.cancelText - Cancel button text (default: 'Cancel')
     * @param {string} options.type - Dialog type: 'danger' (red), 'warning' (yellow), 'info' (blue) (default: 'danger')
     * @param {Function} options.onConfirm - Callback when confirmed
     * @param {Function} options.onCancel - Callback when cancelled
     */
    show(options = {}) {
        return new Promise((resolve) => {
            // Dismiss any existing confirm
            if (this.activeConfirm) {
                this.activeConfirm.hide();
            }

            this.init();

            const {
                title = 'Confirm',
                message = 'Are you sure?',
                confirmText = 'Confirm',
                cancelText = 'Cancel',
                type = 'danger',
                onConfirm = null,
                onCancel = null
            } = options;

            const overlay = document.createElement('div');
            overlay.className = 'absolute inset-0 bg-black/50 transition-opacity';
            overlay.style.animation = 'fadeIn 0.2s ease';

            const dialog = document.createElement('div');
            dialog.className = 'relative bg-white dark:bg-slate-800 rounded-xl shadow-2xl w-full max-w-md mx-4 overflow-hidden';
            dialog.style.animation = 'scaleIn 0.2s ease';

            const confirmBtnClass = this.getConfirmButtonClass(type);
            const icon = this.getIcon(type);

            dialog.innerHTML = `
                <div class="p-6">
                    <div class="flex items-start gap-4">
                        <div class="flex-shrink-0">
                            ${icon}
                        </div>
                        <div class="flex-1">
                            <h3 class="text-lg font-semibold text-slate-900 dark:text-slate-100 mb-2">${title}</h3>
                            <p class="text-slate-600 dark:text-slate-400">${message}</p>
                        </div>
                    </div>
                </div>
                <div class="px-6 py-4 bg-slate-50 dark:bg-slate-700/50 flex gap-3 justify-end">
                    <button class="confirm-cancel-btn px-4 py-2 rounded-lg text-slate-700 dark:text-slate-300 hover:bg-slate-200 dark:hover:bg-slate-600 transition-colors">
                        ${cancelText}
                    </button>
                    <button class="confirm-ok-btn ${confirmBtnClass} px-4 py-2 rounded-lg text-white transition-colors">
                        ${confirmText}
                    </button>
                </div>
            `;

            overlay.appendChild(dialog);
            this.container.appendChild(overlay);

            const confirm = {
                overlay,
                dialog,
                hide: () => {
                    overlay.style.animation = 'fadeOut 0.2s ease forwards';
                    dialog.style.animation = 'scaleOut 0.2s ease forwards';
                    setTimeout(() => {
                        if (overlay.parentElement) {
                            overlay.remove();
                        }
                    }, 200);
                    this.activeConfirm = null;
                }
            };

            this.activeConfirm = confirm;

            // Bind events
            dialog.querySelector('.confirm-ok-btn').addEventListener('click', () => {
                confirm.hide();
                resolve(true);
                if (onConfirm) onConfirm();
            });

            dialog.querySelector('.confirm-cancel-btn').addEventListener('click', () => {
                confirm.hide();
                resolve(false);
                if (onCancel) onCancel();
            });

            // Close on overlay click
            overlay.addEventListener('click', (e) => {
                if (e.target === overlay) {
                    confirm.hide();
                    resolve(false);
                    if (onCancel) onCancel();
                }
            });

            // Close on escape
            const escapeHandler = (e) => {
                if (e.key === 'Escape') {
                    confirm.hide();
                    resolve(false);
                    if (onCancel) onCancel();
                    document.removeEventListener('keydown', escapeHandler);
                }
            };
            document.addEventListener('keydown', escapeHandler);
        });
    },

    /**
     * Get confirm button class based on type
     */
    getConfirmButtonClass(type) {
        const classes = {
            danger: 'bg-red-600 hover:bg-red-700',
            warning: 'bg-yellow-600 hover:bg-yellow-700',
            info: 'bg-blue-600 hover:bg-blue-700',
            success: 'bg-green-600 hover:bg-green-700'
        };

        return classes[type] || classes.danger;
    },

    /**
     * Get icon SVG based on type
     */
    getIcon(type) {
        const icons = {
            danger: `<div class="w-12 h-12 rounded-full bg-red-100 dark:bg-red-900/30 flex items-center justify-center">
                <svg class="w-6 h-6 text-red-600 dark:text-red-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"></path>
                </svg>
            </div>`,
            warning: `<div class="w-12 h-12 rounded-full bg-yellow-100 dark:bg-yellow-900/30 flex items-center justify-center">
                <svg class="w-6 h-6 text-yellow-600 dark:text-yellow-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"></path>
                </svg>
            </div>`,
            info: `<div class="w-12 h-12 rounded-full bg-blue-100 dark:bg-blue-900/30 flex items-center justify-center">
                <svg class="w-6 h-6 text-blue-600 dark:text-blue-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path>
                </svg>
            </div>`,
            success: `<div class="w-12 h-12 rounded-full bg-green-100 dark:bg-green-900/30 flex items-center justify-center">
                <svg class="w-6 h-6 text-green-600 dark:text-green-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"></path>
                </svg>
            </div>`
        };

        return icons[type] || icons.danger;
    },

    /**
     * Show danger confirm (for delete actions)
     */
    danger(message, title = 'Confirm', options = {}) {
        return this.show({
            type: 'danger',
            title,
            message,
            ...options
        });
    },

    /**
     * Show warning confirm
     */
    warning(message, title = 'Warning', options = {}) {
        return this.show({
            type: 'warning',
            title,
            message,
            ...options
        });
    },

    /**
     * Show info confirm
     */
    info(message, title = 'Confirm', options = {}) {
        return this.show({
            type: 'info',
            title,
            message,
            ...options
        });
    }
};

// Add animation styles to document
const style = document.createElement('style');
style.textContent = `
    @keyframes fadeIn {
        from { opacity: 0; }
        to { opacity: 1; }
    }

    @keyframes fadeOut {
        from { opacity: 1; }
        to { opacity: 0; }
    }

    @keyframes scaleIn {
        from {
            transform: scale(0.95);
            opacity: 0;
        }
        to {
            transform: scale(1);
            opacity: 1;
        }
    }

    @keyframes scaleOut {
        from {
            transform: scale(1);
            opacity: 1;
        }
        to {
            transform: scale(0.95);
            opacity: 0;
        }
    }
`;
document.head.appendChild(style);

// Export for module use
if (typeof module !== 'undefined' && module.exports) {
    module.exports = Confirm;
}