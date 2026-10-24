/**
 * ANTGO AUTH & ROLE GUARD - BẢO VỆ PHÂN QUYỀN TRUY CẬP TRANG
 * Tự động kiểm tra phiên đăng nhập và vai trò (Role/Mode) ngay trong <head>
 * Chặn ngay lập tức trước khi HTML render nếu không đủ quyền hoặc chưa đăng nhập.
 */
(function() {
    const publicPaths = [
        '',
        '/',
        '/index',
        '/index.html',
        '/interface/landing',
        '/interface/authentication',
        '/authentication.html',
        '/interface/forgotpassword',
        '/forgot_password.html',
        '/interface/unauthorized',
        '/unauthorized.html',
        '/interface/401'
    ];

    let currentPath = window.location.pathname.toLowerCase();
    if (currentPath.length > 1 && currentPath.endsWith('/')) {
        currentPath = currentPath.slice(0, -1);
    }

    const isPublic = publicPaths.includes(currentPath);
    if (isPublic) return;

    const token = localStorage.getItem('accessToken');
    const userRole = localStorage.getItem('userRole') || '';
    const isAdmin = userRole === 'ROLE_ADMIN' || localStorage.getItem('isAdmin') === 'true';
    const currentMode = (localStorage.getItem('currentMode') || 'WORKER').toUpperCase();

    // 1. CHƯA ĐĂNG NHẬP -> ĐƯA RA TRANG LỖI 401
    if (!token) {
        const redirectTarget = window.location.pathname + window.location.search + window.location.hash;
        window.location.replace('/interface/unauthorized?code=401&redirect=' + encodeURIComponent(redirectTarget));
        return;
    }

    const isAdminPath = currentPath.includes('/interface/admin') || currentPath.includes('/admin.html');

    // 2. TÀI KHOẢN ADMIN: CHỈ ĐƯỢC PHÉP TRUY CẬP TRANG ADMIN
    if (isAdmin) {
        if (!isAdminPath) {
            // Admin cố tình bấm link vào trang người dùng (profile, findjob, managejob...)
            window.location.replace('/interface/unauthorized?code=403&reason=admin_no_user_access&target=' + encodeURIComponent(currentPath));
            return;
        }
        return; // Admin vào trang admin -> Hợp lệ
    }

    // 3. TÀI KHOẢN NGƯỜI DÙNG THƯỜNG: CẤM VÀO TRANG ADMIN
    if (isAdminPath) {
        window.location.replace('/interface/unauthorized?code=403&reason=user_no_admin_access&target=' + encodeURIComponent(currentPath));
        return;
    }

    // 4. PHÂN QUYỀN VAI TRÒ NGƯỜI DÙNG (EMPLOYER vs WORKER)
    const employerOnlyPaths = [
        '/interface/managejob', '/managejob.html',
        '/interface/createjob', '/createjob.html',
        '/interface/findworker', '/findworker.html',
        '/interface/checkapplication', '/checkapplication.html',
        '/interface/generateqr', '/generate_qr.html'
    ];

    const workerOnlyPaths = [
        '/interface/findjob', '/findjob.html',
        '/interface/manageapplication', '/manageapplication.html',
        '/interface/scanner', '/scanner.html'
    ];

    const isEmployerPage = employerOnlyPaths.some(p => currentPath.includes(p));
    const isWorkerPage = workerOnlyPaths.some(p => currentPath.includes(p));

    if (isEmployerPage && currentMode !== 'EMPLOYER') {
        window.location.replace('/interface/unauthorized?code=403&reason=employer_only&target=' + encodeURIComponent(currentPath));
        return;
    }

    if (isWorkerPage && currentMode !== 'WORKER') {
        window.location.replace('/interface/unauthorized?code=403&reason=worker_only&target=' + encodeURIComponent(currentPath));
        return;
    }

    // Tiện ích toàn cục để các hàm fetch API gọi khi gặp HTTP 401 hoặc 403
    window.handleUnauthorizedRedirect = function(customRedirectUrl) {
        const returnUrl = customRedirectUrl || (window.location.pathname + window.location.search);
        localStorage.removeItem('accessToken');
        window.location.href = '/interface/unauthorized?code=401&redirect=' + encodeURIComponent(returnUrl);
    };

    window.handleForbiddenRedirect = function(reason) {
        window.location.href = '/interface/unauthorized?code=403&reason=' + (reason || 'forbidden') + '&target=' + encodeURIComponent(currentPath);
    };
})();
