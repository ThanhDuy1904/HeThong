/**
 * api.js – Shared API helper for TEDU Student Management
 * Base URL: http://localhost:8090
 */

const API_BASE = 'http://localhost:8090/api';

// ── Auth helpers ──────────────────────────────────────────────
function getToken()   { return localStorage.getItem('tedu_token'); }
function getUser()    { return JSON.parse(localStorage.getItem('tedu_user') || 'null'); }
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

function logout() {
    localStorage.removeItem('tedu_token');
    localStorage.removeItem('tedu_user');
    window.location.href = '/index.html';
}

function requireAuth() {
    if (!isLoggedIn()) { window.location.href = '/login.html'; return false; }
    return true;
}

function requireAdmin() {
    if (!isLoggedIn()) { window.location.href = '/login.html'; return false; }
    if (!isAdmin())    { window.location.href = '/user/dashboard.html'; return false; }
    return true;
}

// ── HTTP core ─────────────────────────────────────────────────
async function http(method, path, body = null) {
    const headers = { 'Content-Type': 'application/json' };
    const token = getToken();
    if (token) headers['Authorization'] = `Bearer ${token}`;

    const options = { method, headers };
    if (body) options.body = JSON.stringify(body);

    const res = await fetch(API_BASE + path, options);
    const json = await res.json();
    if (!res.ok) throw new Error(json.message || 'Lỗi server');
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

    // Classes
    classes: {
        getAll:  ()        => http('GET', '/classes'),
        getById: (id)      => http('GET', `/classes/${id}`),
        create:  (data)    => http('POST', '/classes', data),
        update:  (id, data)=> http('PUT', `/classes/${id}`, data),
        updateTuitionFee: (id, tuitionFee) => http('PUT', `/classes/${id}/tuition-fee?tuitionFee=${encodeURIComponent(tuitionFee)}`),
        delete:  (id)      => http('DELETE', `/classes/${id}`),
    },

    posts: {
        getPublished: () => http('GET', '/posts'),
        getAll:       () => http('GET', '/posts?all=true'),
        getById:      (id) => http('GET', `/posts/${id}`),
        create:       (data) => http('POST', '/posts', data),
        update:       (id, data) => http('PUT', `/posts/${id}`, data),
        delete:       (id) => http('DELETE', `/posts/${id}`),
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

    const allowedLabels = {
        ADMIN: null,
        TEACHER: ['Học sinh', 'Giáo viên', 'Lớp học', 'Thời khóa biểu', 'Điểm danh', 'Lịch sử điểm danh', 'Tổng quan'],
        STUDENT: ['Trang chủ', 'Lớp học của tôi', 'Danh sách học sinh', 'Thời khóa biểu', 'Lịch sử điểm danh', 'Bài đăng video'],
        ACCOUNTANT: ['Học sinh'],
        CONTENT_MANAGER: ['Bài đăng video'],
    };

    const allowed = allowedLabels[role];
    if (!allowed) return;

    document.querySelectorAll('.sidebar-nav .nav-item').forEach(item => {
        const label = item.textContent.replace(/\s+/g, ' ').trim();
        const visible = allowed.some(a => label.includes(a));
        item.style.display = visible ? '' : 'none';
    });

    document.querySelectorAll('.sidebar-nav .nav-section').forEach(section => {
        const nextItems = [];
        let node = section.nextElementSibling;
        while (node && !node.classList.contains('nav-section')) {
            if (node.classList.contains('nav-item')) nextItems.push(node);
            node = node.nextElementSibling;
        }
        section.style.display = nextItems.some(el => el.style.display !== 'none') ? '' : 'none';
    });
}
