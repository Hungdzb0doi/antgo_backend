/**
 * ANTGO PROFESSIONAL NOTIFICATION & POPUP SYSTEM
 * Cung cấp hệ thống Modal Popup (thay thế alert, confirm) và Toast Notification
 * Chuẩn nhận diện AntGo: Brand Orange (#FF6600), Dark Surface, Glassmorphism
 */

(function () {
    // 1. Tự động kiểm tra và chèn popup.css nếu trang chưa có
    function ensureStyleLoaded() {
        if (!document.getElementById('antgo-popup-css')) {
            const link = document.createElement('link');
            link.id = 'antgo-popup-css';
            link.rel = 'stylesheet';
            link.href = '/asset/css/popup.css';
            document.head.appendChild(link);
        }
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', ensureStyleLoaded);
    } else {
        ensureStyleLoaded();
    }

    // 2. Icon SVGs sắc nét, đồng bộ (dùng inline SVG để render độc lập, không phụ thuộc font/CDN)
    const ICONS = {
        success: `<svg width="34" height="34" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M20 6L9 17l-5-5"/></svg>`,
        error: `<svg width="34" height="34" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>`,
        warning: `<svg width="34" height="34" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>`,
        info: `<svg width="34" height="34" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="12" y1="16" x2="12" y2="12"/><line x1="12" y1="8" x2="12.01" y2="8"/></svg>`
    };

    const TOAST_ICONS = {
        success: `<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M20 6L9 17l-5-5"/></svg>`,
        error: `<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>`,
        warning: `<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>`,
        info: `<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="12" y1="16" x2="12" y2="12"/><line x1="12" y1="8" x2="12.01" y2="8"/></svg>`
    };

    // 3. Hàm phân tích tự động sắc thái thông báo (Type Detection)
    function detectType(message) {
        if (!message) return 'info';
        const str = String(message).toLowerCase();
        if (str.includes('thành công') || str.includes('hoàn tất') || str.includes('hài lòng') || str.includes('chấp nhận') || str.includes('đã duyệt') || str.includes('đã lưu') || str.includes('success')) {
            return 'success';
        }
        if (str.includes('lỗi') || str.includes('thất bại') || str.includes('không thể') || str.includes('hết hạn') || str.includes('sai mật khẩu') || str.includes('bị khóa') || str.includes('từ chối') || str.includes('error') || str.includes('failed')) {
            return 'error';
        }
        if (str.includes('vui lòng') || str.includes('cảnh báo') || str.includes('chú ý') || str.includes('chắc chắn') || str.includes('xác nhận') || str.includes('bạn có muốn') || str.includes('chưa') || str.includes('warning')) {
            return 'warning';
        }
        return 'info';
    }

    function getDefaultTitle(type) {
        switch (type) {
            case 'success': return 'Thành công';
            case 'error': return 'Thông báo lỗi';
            case 'warning': return 'Lưu ý';
            case 'info':
            default: return 'Thông báo';
        }
    }

    // Container cho Toast
    let toastContainer = null;
    function getToastContainer() {
        if (!toastContainer || !document.body.contains(toastContainer)) {
            toastContainer = document.createElement('div');
            toastContainer.className = 'antgo-toast-container';
            document.body.appendChild(toastContainer);
        }
        return toastContainer;
    }

    /**
     * Hiển thị Toast góc trên bên phải
     * @param {string} message - Nội dung thông báo
     * @param {'success'|'error'|'warning'|'info'} [type] - Loại thông báo (tự động nhận diện nếu bỏ trống)
     * @param {number} [duration=3500] - Thời gian hiển thị (ms)
     */
    window.showToast = function (message, type, duration = 3500) {
        ensureStyleLoaded();
        const detectedType = type || detectType(message);
        const container = getToastContainer();

        const toast = document.createElement('div');
        toast.className = `antgo-toast-item ${detectedType}`;

        const iconWrap = document.createElement('span');
        iconWrap.className = 'antgo-toast-icon';
        iconWrap.innerHTML = TOAST_ICONS[detectedType] || TOAST_ICONS.info;

        const body = document.createElement('div');
        body.className = 'antgo-toast-body';
        body.textContent = String(message || '');

        const closeBtn = document.createElement('button');
        closeBtn.className = 'antgo-toast-close';
        closeBtn.innerHTML = '&times;';
        closeBtn.setAttribute('aria-label', 'Đóng');

        const progressBar = document.createElement('div');
        progressBar.className = 'antgo-toast-progress';
        progressBar.style.transition = `transform ${duration}ms linear`;
        progressBar.style.transform = 'scaleX(1)';

        toast.appendChild(iconWrap);
        toast.appendChild(body);
        toast.appendChild(closeBtn);
        toast.appendChild(progressBar);
        container.appendChild(toast);

        // Kích hoạt animation hiện
        requestAnimationFrame(() => {
            toast.classList.add('antgo-toast-visible');
            requestAnimationFrame(() => {
                progressBar.style.transform = 'scaleX(0)';
            });
        });

        let timer = null;
        const removeToast = () => {
            if (timer) clearTimeout(timer);
            toast.classList.remove('antgo-toast-visible');
            toast.classList.add('antgo-toast-hiding');
            setTimeout(() => {
                if (toast.parentNode) {
                    toast.parentNode.removeChild(toast);
                }
            }, 350);
        };

        closeBtn.addEventListener('click', removeToast);
        timer = setTimeout(removeToast, duration);
    };

    /**
     * Hiển thị Modal Dialog Popup AntGo (thay thế alert/confirm)
     * @param {Object} options
     * @param {string} [options.title] - Tiêu đề
     * @param {string} options.message - Nội dung
     * @param {'success'|'error'|'warning'|'info'} [options.type] - Loại popup
     * @param {string} [options.confirmText='Đồng ý'] - Chữ trên nút xác nhận
     * @param {string} [options.cancelText='Hủy bỏ'] - Chữ trên nút hủy
     * @param {boolean} [options.showCancel=false] - Hiển thị nút Hủy (chế độ confirm)
     * @param {Function} [options.onConfirm] - Callback khi bấm Đồng ý
     * @param {Function} [options.onCancel] - Callback khi bấm Hủy
     * @returns {Promise<boolean>}
     */
    window.showPopup = function (options) {
        ensureStyleLoaded();
        if (typeof options === 'string') {
            options = { message: options };
        }
        options = options || {};

        const rawMessage = options.message || '';
        const type = options.type || detectType(rawMessage);
        const title = options.title || getDefaultTitle(type);
        const confirmText = options.confirmText || 'Đồng ý';
        const cancelText = options.cancelText || 'Hủy bỏ';
        const showCancel = Boolean(options.showCancel);

        return new Promise((resolve) => {
            const backdrop = document.createElement('div');
            backdrop.className = 'antgo-modal-backdrop';

            const card = document.createElement('div');
            card.className = 'antgo-modal-card';

            // Icon Badge
            const iconBadge = document.createElement('div');
            iconBadge.className = `antgo-modal-icon-badge ${type}`;
            iconBadge.innerHTML = ICONS[type] || ICONS.info;

            // Title
            const titleEl = document.createElement('h3');
            titleEl.className = 'antgo-modal-title';
            titleEl.textContent = title;

            // Message
            const msgEl = document.createElement('div');
            msgEl.className = 'antgo-modal-message';
            msgEl.textContent = String(rawMessage);

            // Actions Container
            const actions = document.createElement('div');
            actions.className = 'antgo-modal-actions';

            let cancelBtn = null;
            if (showCancel) {
                cancelBtn = document.createElement('button');
                cancelBtn.type = 'button';
                cancelBtn.className = 'antgo-modal-btn antgo-modal-btn-cancel';
                cancelBtn.textContent = cancelText;
                actions.appendChild(cancelBtn);
            }

            const confirmBtn = document.createElement('button');
            confirmBtn.type = 'button';
            confirmBtn.className = 'antgo-modal-btn antgo-modal-btn-confirm';
            confirmBtn.textContent = confirmText;
            actions.appendChild(confirmBtn);

            card.appendChild(iconBadge);
            card.appendChild(titleEl);
            card.appendChild(msgEl);
            card.appendChild(actions);
            backdrop.appendChild(card);
            document.body.appendChild(backdrop);

            // Tránh scroll body khi popup đang mở
            const originalOverflow = document.body.style.overflow;
            document.body.style.overflow = 'hidden';

            // Kích hoạt transition hiện
            requestAnimationFrame(() => {
                backdrop.classList.add('antgo-modal-show');
                confirmBtn.focus();
            });

            let isClosed = false;
            function close(result) {
                if (isClosed) return;
                isClosed = true;
                document.body.style.overflow = originalOverflow;
                backdrop.classList.remove('antgo-modal-show');
                document.removeEventListener('keydown', handleKeyDown);

                setTimeout(() => {
                    if (backdrop.parentNode) {
                        backdrop.parentNode.removeChild(backdrop);
                    }
                    if (result && typeof options.onConfirm === 'function') {
                        options.onConfirm();
                    } else if (!result && typeof options.onCancel === 'function') {
                        options.onCancel();
                    }
                    resolve(result);
                }, 260);
            }

            confirmBtn.addEventListener('click', () => close(true));

            if (cancelBtn) {
                cancelBtn.addEventListener('click', () => close(false));
            }

            // Đóng khi click ra vùng backdrop tối bên ngoài
            backdrop.addEventListener('click', (e) => {
                if (e.target === backdrop) {
                    close(showCancel ? false : true);
                }
            });

            // Bắt phím Enter / Escape
            function handleKeyDown(e) {
                if (e.key === 'Escape') {
                    e.preventDefault();
                    close(showCancel ? false : true);
                } else if (e.key === 'Enter') {
                    e.preventDefault();
                    close(true);
                }
            }
            document.addEventListener('keydown', handleKeyDown);
        });
    };

    /**
     * Thay thế confirm(...) truyền thống bằng AntGo Confirm Popup hiện đại
     * Trả về Promise<boolean> (true nếu người dùng ấn Đồng ý, false nếu ấn Hủy bỏ)
     * @param {string} message - Câu hỏi xác nhận
     * @param {Object} [options] - Tuỳ chọn thêm: { title, confirmText, cancelText, type }
     * @returns {Promise<boolean>}
     */
    window.showConfirm = function (message, options = {}) {
        return window.showPopup({
            title: options.title || 'Xác nhận hành động',
            message: message,
            type: options.type || 'warning',
            showCancel: true,
            confirmText: options.confirmText || 'Đồng ý',
            cancelText: options.cancelText || 'Hủy bỏ',
            ...options
        });
    };

    /**
     * Thay thế window.alert() mặc định của trình duyệt
     * Hỗ trợ gọi callback khi ấn Đồng ý: alert(message, callback) hoặc await alert(message)
     */
    const _originalAlert = window.alert;
    window.alert = function (message, onConfirm) {
        if (typeof onConfirm === 'function') {
            return window.showPopup({
                message: message,
                onConfirm: onConfirm
            });
        }
        if (typeof onConfirm === 'object' && onConfirm !== null) {
            return window.showPopup({
                message: message,
                ...onConfirm
            });
        }
        return window.showPopup({ message: message });
    };

    // Tiện ích tắt nhanh (Shorthand methods)
    window.showSuccess = function (message, title = 'Thành công') {
        return window.showPopup({ title, message, type: 'success' });
    };

    window.showError = function (message, title = 'Có lỗi xảy ra') {
        return window.showPopup({ title, message, type: 'error' });
    };

    window.showWarning = function (message, title = 'Lưu ý') {
        return window.showPopup({ title, message, type: 'warning' });
    };

    window.showInfo = function (message, title = 'Thông báo') {
        return window.showPopup({ title, message, type: 'info' });
    };
})();
