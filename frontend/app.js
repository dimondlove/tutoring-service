/* Общие функции связи интерфейса с REST API. Все запросы идут через Nginx: /api -> backend. */
(() => {
  const TOKEN_KEY = 'tutoring_access_token';
  const USER_KEY = 'tutoring_user';

  function getToken() { return localStorage.getItem(TOKEN_KEY); }
  function getUser() {
    try { return JSON.parse(localStorage.getItem(USER_KEY) || 'null'); }
    catch { return null; }
  }
  function saveAuth(payload) {
    if (!payload?.token || !payload?.user) throw new Error('Сервер вернул неполные данные авторизации');
    localStorage.setItem(TOKEN_KEY, payload.token);
    localStorage.setItem(USER_KEY, JSON.stringify(payload.user));
  }
  function logout() {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    location.href = 'index.html';
  }
  function pageForRole(role) {
    return ({ STUDENT: 'student.html', TUTOR: 'tutor.html', ADMIN: 'admin.html' })[role] || 'index.html';
  }
  function requireRole(role) {
    const user = getUser();
    if (!getToken() || !user) {
      location.href = 'index.html?login=1';
      return null;
    }
    if (role && user.role !== role) {
      location.href = pageForRole(user.role);
      return null;
    }
    return user;
  }
  async function request(path, options = {}) {
    const headers = new Headers(options.headers || {});
    if (options.body && !(options.body instanceof FormData)) headers.set('Content-Type', 'application/json');
    if (getToken()) headers.set('Authorization', `Bearer ${getToken()}`);
    const response = await fetch(`/api${path}`, { ...options, headers });
    if (response.status === 401 && getToken()) {
      localStorage.removeItem(TOKEN_KEY);
      localStorage.removeItem(USER_KEY);
    }
    if (response.status === 204) return null;
    const contentType = response.headers.get('content-type') || '';
    const payload = contentType.includes('application/json') ? await response.json() : await response.text();
    if (!response.ok) {
      const message = payload && typeof payload === 'object'
        ? (payload.message || payload.error || `Ошибка ${response.status}`)
        : (payload || `Ошибка ${response.status}`);
      const error = new Error(message);
      error.status = response.status;
      error.code = payload?.code;
      error.details = payload?.details;
      throw error;
    }
    return payload;
  }
  const json = (method, data) => ({ method, body: JSON.stringify(data) });
  function escapeHtml(value) {
    return String(value ?? '').replace(/[&<>"']/g, ch => ({ '&':'&amp;', '<':'&lt;', '>':'&gt;', '"':'&quot;', "'":'&#39;' })[ch]);
  }
  function localDate(value = new Date()) {
    return new Intl.DateTimeFormat('en-CA', { timeZone: 'Europe/Moscow', year: 'numeric', month: '2-digit', day: '2-digit' }).format(value);
  }
  function formatDateTime(value) {
    if (!value) return '—';
    return new Intl.DateTimeFormat('ru-RU', { timeZone: 'Europe/Moscow', dateStyle: 'short', timeStyle: 'short' }).format(new Date(value));
  }
  function formatDate(value) {
    if (!value) return '—';
    // API dates without time are interpreted as calendar dates, not UTC instants.
    if (/^\d{4}-\d{2}-\d{2}$/.test(value)) {
      const [y, m, d] = value.split('-').map(Number);
      return new Date(y, m - 1, d).toLocaleDateString('ru-RU');
    }
    return new Intl.DateTimeFormat('ru-RU', { timeZone: 'Europe/Moscow', dateStyle: 'short' }).format(new Date(value));
  }
  function datePlusDays(days) {
    const [y, m, d] = localDate().split('-').map(Number);
    const date = new Date(Date.UTC(y, m - 1, d + days));
    return date.toISOString().slice(0, 10);
  }
  function toast(message) {
    const node = document.createElement('div');
    node.className = 'toast';
    node.textContent = message;
    document.body.appendChild(node);
    requestAnimationFrame(() => node.classList.add('show'));
    setTimeout(() => { node.classList.remove('show'); setTimeout(() => node.remove(), 300); }, 3200);
  }
  function showError(error) {
    console.error(error);
    toast(error?.message || 'Не удалось выполнить запрос');
  }
  function wireLogout() {
    document.querySelectorAll('[data-logout]').forEach(el => el.addEventListener('click', event => {
      event.preventDefault(); logout();
    }));
  }

  window.App = { getToken, getUser, saveAuth, logout, pageForRole, requireRole, request, json,
    escapeHtml, localDate, datePlusDays, formatDate, formatDateTime, toast, showError, wireLogout };
})();
