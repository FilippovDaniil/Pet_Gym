const API = '/api';

async function apiCall(method, path, body, token) {
    const opts = { method, headers: { 'Content-Type': 'application/json' } };
    if (token) opts.headers['Authorization'] = 'Bearer ' + token;
    if (body) opts.body = JSON.stringify(body);
    const res = await fetch(API + path, opts);
    const data = await res.json().catch(() => ({}));
    if (!res.ok) throw new Error(data.message || res.statusText);
    return data;
}

document.getElementById('login-form')?.addEventListener('submit', async e => {
    e.preventDefault();
    const email = document.getElementById('email').value;
    const password = document.getElementById('password').value;
    const errEl = document.getElementById('error-msg');
    try {
        const data = await apiCall('POST', '/auth/login', { email, password });
        localStorage.setItem('token', data.token);
        localStorage.setItem('role', data.role);
        localStorage.setItem('userId', data.userId);
        localStorage.setItem('userName', data.firstName + ' ' + data.lastName);
        const roleMap = { CLIENT: 'client.html', RECEPTION: 'reception.html', ADMIN: 'admin.html', TRAINER: 'trainer.html' };
        window.location.href = roleMap[data.role] || 'index.html';
    } catch (err) {
        errEl.textContent = err.message;
        errEl.classList.remove('d-none');
    }
});

document.getElementById('register-form')?.addEventListener('submit', async e => {
    e.preventDefault();
    const errEl = document.getElementById('reg-error');
    const successEl = document.getElementById('reg-success');
    try {
        const data = await apiCall('POST', '/auth/register', {
            email: document.getElementById('reg-email').value,
            password: document.getElementById('reg-password').value,
            firstName: document.getElementById('reg-first').value,
            lastName: document.getElementById('reg-last').value,
            phone: document.getElementById('reg-phone').value
        });
        localStorage.setItem('token', data.token);
        localStorage.setItem('role', data.role);
        localStorage.setItem('userId', data.userId);
        localStorage.setItem('userName', data.firstName + ' ' + data.lastName);
        successEl.textContent = 'Регистрация успешна! Перенаправление...';
        successEl.classList.remove('d-none');
        setTimeout(() => window.location.href = 'client.html', 1000);
    } catch (err) {
        errEl.textContent = err.message;
        errEl.classList.remove('d-none');
    }
});

function checkAuth(expectedRole) {
    const token = localStorage.getItem('token');
    const role = localStorage.getItem('role');
    if (!token || role !== expectedRole) {
        window.location.href = 'index.html';
        return false;
    }
    return true;
}

function logout() {
    localStorage.clear();
    window.location.href = 'index.html';
}

function getToken() { return localStorage.getItem('token'); }
function getUserId() { return localStorage.getItem('userId'); }
function getUserName() { return localStorage.getItem('userName'); }

async function get(path) { return apiCall('GET', path, null, getToken()); }
async function post(path, body) { return apiCall('POST', path, body, getToken()); }
async function put(path, body) { return apiCall('PUT', path, body, getToken()); }
async function del(path) {
    const res = await fetch(API + path, { method: 'DELETE', headers: { 'Authorization': 'Bearer ' + getToken() } });
    if (!res.ok) { const d = await res.json().catch(() => ({})); throw new Error(d.message || res.statusText); }
}

function fmtDt(dt) {
    if (!dt) return '—';
    const d = new Date(dt);
    return d.toLocaleString('ru-RU', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' });
}
function fmtDate(d) {
    if (!d) return '—';
    return new Date(d).toLocaleDateString('ru-RU');
}
function showErr(id, msg) {
    const el = document.getElementById(id);
    if (el) { el.textContent = msg; el.classList.remove('d-none'); setTimeout(() => el.classList.add('d-none'), 5000); }
}
function showOk(id, msg) {
    const el = document.getElementById(id);
    if (el) { el.textContent = msg; el.classList.remove('d-none'); setTimeout(() => el.classList.add('d-none'), 3000); }
}
