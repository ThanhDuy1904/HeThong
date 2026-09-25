/**
 * api.js – Shared API helper for TEDU Student Management
 * Base URL: http://localhost:8090
 */

const API_BASE = window.location.protocol === 'file:'
    ? 'http://localhost:8090/api'
    : ['5500', '3000'].includes(window.location.port)
    ? `${window.location.protocol}//${window.location.hostname}:8090/api`
    : `${window.location.origin}/api`;

// ── Auth helpers ──────────────────────────────────────────────
function getToken()   { return localStorage.getItem('tedu_token'); }
function getUser() {
    try {
        return JSON.parse(localStorage.getItem('tedu_user') || 'null');
    } catch {
        localStorage.removeItem('tedu_user');
        return null;
    }
}
function isAdmin()    { return getUser()?.role === 'ADMIN'; }
function isTeacher()  { return getUser()?.role === 'TEACHER'; }
function isStudent()  { return getUser()?.role === 'STUDENT'; }
function isAccountant() { return getUser()?.role === 'ACCOUNTANT'; }
function isContentManager() { return getUser()?.role === 'CONTENT_MANAGER'; }
function isLoggedIn() { return !!getToken(); }

function saveAuth(data) {
    localStorage.setItem('tedu_token', data.token);
    localStorage.setItem('tedu_user', JSON.stringify({
        id: data.id, username: data.username,
        email: data.email, fullName: data.fullName, role: data.role
    }));
}

function getRoleLandingPath(role = getUser()?.role) {
    switch (role) {
        case 'ADMIN':
            return '/admin/dashboard.html';
        case 'TEACHER':
            return '/teacher/dashboard.html';
        case 'ACCOUNTANT':
            return '/accountant/dashboard.html';
        case 'CONTENT_MANAGER':
            return '/admin/posts.html';
        case 'ACADEMIC_AFFAIRS':
            return '/admin/dashboard.html';
        case 'MANAGER':
            return '/accountant/dashboard.html';
        case 'STUDENT':
            return '/user/dashboard.html';
        default:
            return '/index.html';
    }
}

function redirectToRoleHome(role = getUser()?.role) {
    window.location.replace(getRoleLandingPath(role));
}

function logout() {
    localStorage.removeItem('tedu_token');
    localStorage.removeItem('tedu_user');
    window.location.replace('/login.html');
}

function requireAuth() {
    if (!isLoggedIn()) { window.location.replace('/login.html'); return false; }
    return true;
}

function requireAdmin() {
    if (!isLoggedIn()) { window.location.replace('/login.html'); return false; }
    if (!isAdmin())    { redirectToRoleHome(); return false; }
    return true;
}

// ── HTTP core ─────────────────────────────────────────────────
async function http(method, path, body = null) {
    const headers = {};
    const isMultipart = body instanceof FormData;
    if (!isMultipart) headers['Content-Type'] = 'application/json';
    const token = getToken();
    if (token) headers['Authorization'] = `Bearer ${token}`;

    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), 15000);
    const options = { method, headers, signal: controller.signal };
    if (body) options.body = isMultipart ? body : JSON.stringify(body);

    let res;
    try {
        res = await fetch(API_BASE + path, options);
    } catch (error) {
        if (error.name === 'AbortError') {
            throw new Error('Yêu cầu quá thời gian. Vui lòng thử lại.');
        }
        throw new Error('Không thể kết nối tới máy chủ.');
    } finally {
        clearTimeout(timeoutId);
    }

    const text = await res.text();
    let json = null;
    try {
        json = text ? JSON.parse(text) : null;
    } catch (_) {
        json = null;
    }
    if (!res.ok) {
        if (res.status === 401 && !path.startsWith('/auth/')) {
            localStorage.removeItem('tedu_token');
            localStorage.removeItem('tedu_user');
            if (!window.location.pathname.endsWith('/login.html')) {
                window.location.replace('/login.html');
            }
        }
        const message = json?.message || json?.error || text || 'Lỗi server';
        throw new Error(message);
    }
    return json;
}

const api = {
    // Auth
    login:    (data) => http('POST', '/auth/login', data),
    register: (data) => http('POST', '/auth/register', data),

    // Admin dashboard
    dashboard: () => http('GET', '/admin/dashboard'),

    // Students
    students: {
        getAll:   (kw = '') => http('GET', `/students${kw ? '?keyword='+encodeURIComponent(kw) : ''}`),
        getMe:    ()        => http('GET', '/students/me'),
        getById:  (id)      => http('GET', `/students/${id}`),
        create:   (data)    => http('POST', '/students', data),
        update:   (id, data)=> http('PUT', `/students/${id}`, data),
        delete:   (id)      => http('DELETE', `/students/${id}`),
        byClass:  (cid)     => http('GET', `/students/class/${cid}`),
        removeFromClass: (id) => http('PATCH', `/students/${id}/remove-from-class`),
        importFile: (formData) => http('POST', '/students/import', formData),
    },

    // Teachers
    teachers: {
        getAll:  (kw = '') => http('GET', `/teachers${kw ? '?keyword='+encodeURIComponent(kw) : ''}`),
        getMe:   ()        => http('GET', '/teachers/me'),
        getById: (id)      => http('GET', `/teachers/${id}`),
        create:  (data)    => http('POST', '/teachers', data),
        update:  (id, data)=> http('PUT', `/teachers/${id}`, data),
        delete:  (id)      => http('DELETE', `/teachers/${id}`),
    },

    accounts: {
        getAll: () => http('GET', '/user/accounts'),
        create: (data) => http('POST', '/user/accounts', data),
        update: (id, data) => http('PUT', `/user/accounts/${id}`, data),
        delete: (id) => http('DELETE', `/user/accounts/${id}`),
    },
    archive: {
        list: () => http('GET', '/archive'),
        upload: (file) => { const form = new FormData(); form.append('file', file); return http('POST', '/archive', form); },
        viewUrl: (id) => `${API_BASE}/archive/${id}/view`,
        delete: (id) => http('DELETE', `/archive/${id}`),
        view: async (id) => {
            const response = await fetch(`${API_BASE}/archive/${id}/view`, {
                headers: { Authorization: `Bearer ${getToken()}` }
            });
            if (!response.ok) throw new Error('Không thể xem file');
            const url = URL.createObjectURL(await response.blob());
            window.open(url, '_blank');
            setTimeout(() => URL.revokeObjectURL(url), 60000);
        },
        download: async (id, name) => {
            const response = await fetch(`${API_BASE}/archive/${id}/download`, {
                headers: { Authorization: `Bearer ${getToken()}` }
            });
            if (!response.ok) throw new Error('Không thể tải file');
            const blob = await response.blob();
            const url = URL.createObjectURL(blob);
            const link = document.createElement('a');
            link.href = url; link.download = name; link.click();
            URL.revokeObjectURL(url);
        },
    },
    teachingSessions: {
        getWeek: (from, to) => http('GET', `/teaching-sessions?from=${from}&to=${to}`),
        getStats: (from, to) => http('GET', `/teaching-sessions/stats?from=${from}&to=${to}`),
        update: (id, data) => http('PUT', `/teaching-sessions/${id}`, data),
    },

    // Classes
    classes: {
        getAll:  ()        => http('GET', '/classes'),
        getById: (id)      => http('GET', `/classes/${id}`),
        create:  (data)    => http('POST', '/classes', data),
        update:  (id, data)=> http('PUT', `/classes/${id}`, data),
        updateTuitionFee: (id, tuitionFee) => http('PUT', `/classes/${id}/tuition-fee?tuitionFee=${encodeURIComponent(tuitionFee)}`),
        delete:  (id)      => http('DELETE', `/classes/${id}`),
        archive: (id, archived = true) => http('PATCH', `/classes/${id}/archive?archived=${archived}`),
    },

    posts: {
        getPublished: () => http('GET', '/posts'),
        getAll:       (includeDeleted = false) => http('GET', `/posts?all=true${includeDeleted ? '&includeDeleted=true' : ''}`),
        getById:      (id) => http('GET', `/posts/${id}`),
        create:       (data) => http('POST', '/posts', data),
        update:       (id, data) => http('PUT', `/posts/${id}`, data),
        delete:       (id) => http('DELETE', `/posts/${id}`),
        toggleVisibility: (id) => http('PATCH', `/posts/${id}/visibility`),
        togglePinned:     (id) => http('PATCH', `/posts/${id}/pinned`),
        restore:          (id) => http('PATCH', `/posts/${id}/restore`),
    },

    // Tuition payments
    tuitionPayments: {
        getMy:      () => http('GET', '/tuition-payments/me'),
        getByStudent: (studentId) => http('GET', `/tuition-payments/student/${studentId}`),
        getByClass: (classId) => http('GET', `/tuition-payments/class/${classId}`),
        collect: (studentId, data) => http('POST', `/tuition-payments/student/${studentId}/collect`, data),
        closeClass: (classId, fileName, format = 'pdf') => http('POST', `/tuition-payments/class/${classId}/close?fileName=${encodeURIComponent(fileName)}&format=${format}`),
    },
    revenue: {
        report: (schoolYear, year, month) => {
            const params = new URLSearchParams();
            if (schoolYear) params.set('schoolYear', schoolYear);
            if (year) params.set('year', year);
            if (month) params.set('month', month);
            return http('GET', `/revenue?${params.toString()}`);
        },
        expenses: {
            list: () => http('GET', '/expenses'),
            available: () => http('GET', '/expenses/available'),
            create: (data) => http('POST', '/expenses', data),
            delete: (id) => http('DELETE', `/expenses/${id}`),
            imageUrl: (id) => `${API_BASE}/expenses/${id}/image`,
            viewImage: async (id) => {
                const response = await fetch(`${API_BASE}/expenses/${id}/image`, { headers: { Authorization: `Bearer ${getToken()}` } });
                if (!response.ok) throw new Error('Không thể xem hình ảnh');
                const url = URL.createObjectURL(await response.blob());
                window.open(url, '_blank');
                setTimeout(() => URL.revokeObjectURL(url), 60000);
            },
        },
    },

    // Schedules (Lịch học)
    schedules: {
        getMy:        ()        => http('GET', '/schedules/my'),
        getByClass:   (classId) => http('GET', `/schedules/class/${classId}`),
        getByTeacher: (teacherId) => http('GET', `/schedules/teacher/${teacherId}`),
        getById:      (id)      => http('GET', `/schedules/${id}`),
        create:       (data)    => http('POST', '/schedules', data),
        update:       (id, data)=> http('PUT', `/schedules/${id}`, data),
        delete:       (id)      => http('DELETE', `/schedules/${id}`),
    },

    // Attendances (Điểm danh)
    attendances: {
        getByScheduleAndDate: (scheduleId, date) =>
            http('GET', `/attendances/schedule/${scheduleId}?date=${date}`),
        getByStudent: (studentId) => http('GET', `/attendances/student/${studentId}`),
        mark:         (data)      => http('POST', '/attendances/mark', data),
    },

    // User profile management
    user: {
        getProfile:       ()        => http('GET', '/user/profile'),
        updateProfile:    (data)    => http('PUT', '/user/profile', data),
        changePassword:   (data)    => http('PUT', '/user/change-password', data),
    },
};

// ── Toast notification ────────────────────────────────────────
function showToast(message, type = 'success') {
    const existing = document.getElementById('toast-container');
    if (!existing) {
        const c = document.createElement('div');
        c.id = 'toast-container';
        c.style.cssText = 'position:fixed;top:20px;right:20px;z-index:9999;display:flex;flex-direction:column;gap:8px';
        document.body.appendChild(c);
    }
    const toast = document.createElement('div');
    toast.style.cssText = `padding:12px 20px;border-radius:8px;color:#fff;font-size:.9rem;
        box-shadow:0 4px 12px rgba(0,0,0,.2);min-width:240px;
        background:${type === 'success' ? '#2d9e5c' : type === 'error' ? '#f72585' : '#4361ee'};
        animation:slideIn .3s ease`;
    toast.textContent = message;
    document.getElementById('toast-container').appendChild(toast);
    setTimeout(() => toast.remove(), 3500);
}

// ── Modal helpers ─────────────────────────────────────────────
function openModal(id)  {
    const el = document.getElementById(id);
    if (el) el.classList.add('show');
}
function closeModal(id) {
    const el = document.getElementById(id);
    if (el) el.classList.remove('show');
}

// ── Confirm dialog ─────────────────────────────────────────────
function confirmDelete(callback) {
    if (confirm('Bạn có chắc chắn muốn xóa? Hành động này không thể hoàn tác.')) {
        callback();
    }
}

// ── Inject sidebar user info ──────────────────────────────────
function renderUserInfo() {
    const user = getUser();
    if (!user) return;
    const nameEl = document.getElementById('sidebar-username');
    const roleEl = document.getElementById('sidebar-role');
    if (nameEl) nameEl.textContent = user.fullName || user.username;
    if (roleEl) roleEl.textContent = user.role;
    renderRoleNavigation();
}

function renderRoleNavigation() {
    const role = getUser()?.role;
    if (!role) return;

    const menus = {
        ADMIN: [
            ['Tổng quan', '/admin/dashboard.html', 'tachometer-alt'],
            ['Học sinh', '/admin/students.html', 'user-graduate'],
            ['Giáo viên', '/admin/teachers.html', 'chalkboard-teacher'],
            ['Lớp học', '/admin/classes.html', 'school'],
            ['Thời khóa biểu', '/admin/timetable.html', 'calendar'],
            ['Điểm danh', '/admin/attendance.html', 'check-circle'],
            ['Quản lý tài khoản', '/admin/accounts.html', 'users-cog'],
            ['Lưu trữ', '/admin/archive.html', 'folder-open'],
            ['Doanh thu', '/admin/revenue.html', 'chart-bar'],
            ['Chi tiêu', '/admin/expenses.html', 'receipt'],
            ['Lịch dạy giáo viên', '/admin/teaching-schedule.html', 'chalkboard-teacher'],
            ['Bài đăng video', '/admin/posts.html', 'video']
        ],
        ACADEMIC_AFFAIRS: [
            ['Học sinh', '/admin/students.html', 'user-graduate'],
            ['Giáo viên', '/admin/teachers.html', 'chalkboard-teacher'],
            ['Lớp học', '/admin/classes.html', 'school'],
            ['Thời khóa biểu', '/admin/timetable.html', 'calendar'],
            ['Điểm danh', '/admin/attendance.html', 'check-circle']
            ,['Lưu trữ', '/admin/archive.html', 'folder-open']
            ,['Lịch dạy giáo viên', '/admin/teaching-schedule.html', 'chalkboard-teacher']
        ],
        MANAGER: [
            ['Quản lý học phí', '/accountant/dashboard.html', 'money-bill-wave']
            ,['Lưu trữ', '/admin/archive.html', 'folder-open']
            ,['Doanh thu', '/admin/revenue.html', 'chart-bar']
            ,['Chi tiêu', '/admin/expenses.html', 'receipt']
        ],
        TEACHER: [
            ['Tổng quan', '/teacher/dashboard.html', 'home'],
            ['Học sinh', '/teacher/students.html', 'user-graduate'],
            ['Lớp học', '/teacher/classes.html', 'school'],
            ['Thời khóa biểu', '/teacher/timetable.html', 'calendar'],
            ['Điểm danh', '/teacher/attendance.html', 'check-circle'],
            ['Lưu trữ', '/admin/archive.html', 'folder-open']
        ]
    };
    const menu = menus[role];
    const nav = document.querySelector('.sidebar-nav');
    if (!menu || !nav) return;
    const currentPath = window.location.pathname;
    nav.innerHTML = `<div class="nav-section">${role === 'TEACHER' ? 'Chính' : 'Quản lý'}</div>` +
        menu.map(([label, href, icon]) => `
            <div class="nav-item${currentPath.endsWith(href) ? ' active' : ''}" data-nav-href="${href}">
                <i class="fa fa-${icon}"></i> ${label}
            </div>`).join('');
    nav.querySelectorAll('[data-nav-href]').forEach(item => {
        item.addEventListener('click', () => window.location.assign(item.dataset.navHref));
    });
    const brand = document.querySelector('.sidebar-brand');
    if (brand && role === 'TEACHER') brand.innerHTML = '<span>🎓</span> TEDU Teacher';
}

function setupMobileNavigation() {
    const layout = document.querySelector('.layout');
    const sidebar = document.querySelector('.sidebar');
    const topbar = document.querySelector('.topbar');
    if (!layout || !sidebar || !topbar || document.querySelector('.mobile-menu-toggle')) return;

    const toggle = document.createElement('button');
    toggle.type = 'button';
    toggle.className = 'mobile-menu-toggle';
    toggle.setAttribute('aria-label', 'Mở menu');
    toggle.innerHTML = '<i class="fa fa-bars"></i>';

    const overlay = document.createElement('div');
    overlay.className = 'sidebar-overlay';
    layout.appendChild(overlay);
    topbar.prepend(toggle);

    const close = () => {
        sidebar.classList.remove('open');
        overlay.classList.remove('show');
        toggle.setAttribute('aria-label', 'Mở menu');
        toggle.innerHTML = '<i class="fa fa-bars"></i>';
    };
    toggle.addEventListener('click', () => {
        const open = sidebar.classList.toggle('open');
        overlay.classList.toggle('show', open);
        toggle.setAttribute('aria-label', open ? 'Đóng menu' : 'Mở menu');
        toggle.innerHTML = `<i class="fa fa-${open ? 'times' : 'bars'}"></i>`;
    });
    overlay.addEventListener('click', close);
    sidebar.querySelectorAll('.nav-item').forEach(item => item.addEventListener('click', close));
}

document.addEventListener('DOMContentLoaded', setupMobileNavigation);
