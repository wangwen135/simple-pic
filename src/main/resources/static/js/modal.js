/**
 * Modal Dialog Component
 * 通用模态弹框组件
 */
const Modal = {
    // Show modal
    show: function(options) {
        const {
            title = '',
            content = '',
            onConfirm = null,
            confirmText = '确认',
            cancelText = '取消',
            showCancel = true,
            size = 'medium', // small, medium, large
            closable = true
        } = options;

        // Remove existing modal
        this.close();

        // Create modal overlay
        const overlay = document.createElement('div');
        overlay.id = 'modal-overlay';
        overlay.className = 'fixed inset-0 bg-black/50 flex items-center justify-center z-[10000] transition-opacity';
        overlay.onclick = (e) => {
            if (e.target === overlay && closable) {
                this.close();
            }
        };

        // Size classes
        const sizeClasses = {
            small: 'max-w-sm',
            medium: 'max-w-md',
            large: 'max-w-lg',
            xlarge: 'max-w-xl'
        };

        // Create modal content
        const modal = document.createElement('div');
        modal.className = `modal glass rounded-xl p-6 w-full ${sizeClasses[size] || sizeClasses.medium} mx-4 transform transition-all`;
        modal.onclick = (e) => e.stopPropagation();

        // Create header with title and close button
        let headerHtml = '';
        if (title || closable) {
            headerHtml = '<div class="flex items-center justify-between mb-4">';
            if (title) {
                headerHtml += `<h2 class="text-xl font-bold text-primary" id="modal-title">${title}</h2>`;
            } else {
                headerHtml += '<div></div>';
            }
            if (closable) {
                headerHtml += `
                    <button onclick="Modal.close()" class="p-1 rounded-lg hover:bg-slate-700/50 transition-colors text-muted hover:text-primary">
                        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path>
                        </svg>
                    </button>
                `;
            }
            headerHtml += '</div>';
        }

        // Create buttons
        let buttonsHtml = '';
        if (showCancel) {
            buttonsHtml += `<button id="modal-cancel" class="w-full px-4 py-2 btn-secondary rounded-lg mb-2">${cancelText}</button>`;
        }
        if (onConfirm) {
            buttonsHtml += `<button id="modal-confirm" class="w-full px-4 py-2 btn-primary text-white rounded-lg">${confirmText}</button>`;
        }

        modal.innerHTML = `
            ${headerHtml}
            <div id="modal-content" class="mb-4">${content}</div>
            <div id="modal-buttons" class="space-y-2">${buttonsHtml}</div>
        `;

        overlay.appendChild(modal);
        document.body.appendChild(overlay);

        // Apply language
        if (typeof i18n !== 'undefined' && i18n.applyLanguage) {
            i18n.applyLanguage();
        }

        // Bind events
        const confirmBtn = document.getElementById('modal-confirm');
        const cancelBtn = document.getElementById('modal-cancel');

        if (confirmBtn && onConfirm) {
            confirmBtn.onclick = async () => {
                const result = onConfirm();
                const finalResult = result instanceof Promise ? await result : result;
                if (finalResult !== false) {
                    this.close();
                }
            };
        }

        if (cancelBtn) {
            cancelBtn.onclick = () => this.close();
        }

        // Prevent body scroll
        document.body.style.overflow = 'hidden';

        // Focus on modal
        setTimeout(() => modal.focus(), 100);

        return {
            element: overlay,
            modal: modal,
            setContent: (html) => {
                document.getElementById('modal-content').innerHTML = html;
            },
            getContent: () => document.getElementById('modal-content'),
            close: () => this.close()
        };
    },

    // Close modal
    close: function() {
        const overlay = document.getElementById('modal-overlay');
        if (overlay) {
            overlay.remove();
            document.body.style.overflow = '';
        }
    },

    // Confirm dialog
    confirm: function(options) {
        return this.show({
            ...options,
            showCancel: true,
            closable: false
        });
    },

    // Alert dialog
    alert: function(options) {
        return this.show({
            ...options,
            showCancel: false,
            closable: true
        });
    }
};

// ESC key to close modal
document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape') {
        Modal.close();
    }
});
