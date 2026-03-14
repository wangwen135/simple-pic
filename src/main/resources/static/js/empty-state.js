/**
 * Empty State Component
 * Provides unified empty state display with visual guidance
 */

const EmptyState = {
    /**
     * Create an empty state element
     * @param {Object} options - Configuration options
     * @param {string} options.title - The title text
     * @param {string} options.message - The message text
     * @param {string} options.icon - The icon SVG string (optional)
     * @param {string} options.actionText - Action button text (optional)
     * @param {Function} options.onAction - Action button callback (optional)
     */
    create(options = {}) {
        const {
            title = '',
            message = '',
            icon = this.getDefaultIcon(),
            actionText = null,
            onAction = null
        } = options;

        const container = document.createElement('div');
        container.className = 'empty-state flex flex-col items-center justify-center py-12 px-4';

        let actionButton = '';
        if (actionText && onAction) {
            actionButton = `<button class="empty-state-action-btn mt-4 px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-lg transition-colors">${actionText}</button>`;
        }

        container.innerHTML = `
            <div class="empty-state-icon mb-4 w-16 h-16 rounded-full bg-slate-200 dark:bg-slate-700 flex items-center justify-center">
                ${icon}
            </div>
            <h3 class="empty-state-title text-lg font-medium text-slate-900 dark:text-slate-100 mb-2">${title}</h3>
            <p class="empty-state-message text-slate-500 dark:text-slate-400 text-center max-w-md">${message}</p>
            ${actionButton}
        `;

        // Bind action button event
        if (actionButton && onAction) {
            const btn = container.querySelector('.empty-state-action-btn');
            if (btn) {
                btn.addEventListener('click', onAction);
            }
        }

        return container;
    },

    /**
     * Get default empty state icon
     */
    getDefaultIcon() {
        return `<svg class="w-8 h-8 text-slate-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20 13V6a2 2 0 00-2-2H6a2 2 0 00-2 2v7m16 0v5a2 2 0 01-2 2H6a2 2 0 01-2-2v-5m16 0h-2.586a1 1 0 00-.707.293l-2.414 2.414a1 1 0 01-.707.293h-3.172a1 1 0 01-.707-.293l-2.414-2.414A1 1 0 006.586 13H4"></path>
        </svg>`;
    },

    /**
     * Get icon by type
     */
    getIcon(type) {
        const icons = {
            default: this.getDefaultIcon(),
            search: `<svg class="w-8 h-8 text-slate-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"></path>
            </svg>`,
            images: `<svg class="w-8 h-8 text-slate-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z"></path>
            </svg>`,
            users: `<svg class="w-8 h-8 text-slate-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z"></path>
            </svg>`,
            storage: `<svg class="w-8 h-8 text-slate-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 19a2 2 0 01-2-2V7a2 2 0 012-2h4l2 2h4a2 2 0 012 2v1M5 19h14a2 2 0 002-2v-5a2 2 0 00-2-2H9a2 2 0 00-2 2v5a2 2 0 01-2 2z"></path>
            </svg>`,
            api: `<svg class="w-8 h-8 text-slate-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 7a2 2 0 012 2m4 0a6 6 0 01-7.743 5.743L11 17H9v2H7v2H4a1 1 0 01-1-1v-2.586a1 1 0 01.293-.707l5.964-5.964A6 6 0 1121 9z"></path>
            </svg>`
        };

        return icons[type] || icons.default;
    },

    /**
     * Create and render empty state in a container
     * @param {string|HTMLElement} container - Container element or selector
     * @param {Object} options - Configuration options
     */
    render(container, options) {
        const el = typeof container === 'string'
            ? document.querySelector(container)
            : container;

        if (!el) {
            console.warn('EmptyState: Container not found', container);
            return null;
        }

        el.innerHTML = '';
        const emptyState = this.create(options);
        el.appendChild(emptyState);
        return emptyState;
    }
};

// Export for module use
if (typeof module !== 'undefined' && module.exports) {
    module.exports = EmptyState;
}