/* =============================================
   BUDGET TRACKER — Core App JS
   ============================================= */

// ==================== STATE ====================
const App = {
  token: localStorage.getItem('bt_token'),
  user: JSON.parse(localStorage.getItem('bt_user') || 'null'),
  currentPage: 'dashboard',
  currency: '₹',
  darkMode: true,
};

// Currency symbols
const CURRENCIES = {
  INR: '₹', USD: '$', EUR: '€', GBP: '£',
  JPY: '¥', AUD: 'A$', CAD: 'C$', SGD: 'S$'
};

// ==================== API SERVICE ====================
const API = {
  BASE: '/api',

  async request(method, path, body = null, isFormData = false) {
    const opts = {
      method,
      headers: { 'Authorization': `Bearer ${App.token}` }
    };
    if (body) {
      if (isFormData) { opts.body = body; }
      else { opts.headers['Content-Type'] = 'application/json'; opts.body = JSON.stringify(body); }
    }
    const res = await fetch(this.BASE + path, opts);
    const data = await res.json().catch(() => ({ success: false, message: 'Network error' }));
    if (!res.ok) {
      // If validation errors are in data.data (object), pass them as JSON string for callers to parse
      if (data.data && typeof data.data === 'object' && !Array.isArray(data.data)) {
        throw new Error(JSON.stringify(data.data));
      }
      throw new Error(data.message || 'Request failed');
    }
    return data;
  },

  get: (path) => API.request('GET', path),
  post: (path, body) => API.request('POST', path, body),
  put: (path, body) => API.request('PUT', path, body),
  patch: (path, body) => API.request('PATCH', path, body),
  delete: (path) => API.request('DELETE', path),
  upload: (path, formData) => API.request('POST', path, formData, true),
};

// ==================== AUTH HELPERS ====================
function isLoggedIn() { return !!App.token; }

function saveAuth(data) {
  App.token = data.token;
  App.user = data;
  localStorage.setItem('bt_token', data.token);
  localStorage.setItem('bt_user', JSON.stringify(data));
  App.currency = CURRENCIES[data.currency] || '₹';
  App.darkMode = data.darkMode;
}

function logout() {
  App.token = null; App.user = null;
  localStorage.removeItem('bt_token');
  localStorage.removeItem('bt_user');
  showAuthSection();
  showPage('login-page');
}

// ==================== NAVIGATION ====================
function showAuthSection() {
  document.getElementById('auth-section').classList.remove('hidden');
  document.getElementById('app-section').classList.add('hidden');
  document.getElementById('fab-btn').classList.add('hidden');
}

function showAppSection() {
  document.getElementById('auth-section').classList.add('hidden');
  document.getElementById('app-section').classList.remove('hidden');
  document.getElementById('fab-btn').classList.remove('hidden');
  updateUserUI();
  applyDarkMode(App.darkMode);
  App.currency = CURRENCIES[App.user?.currency] || '₹';
  document.getElementById('topbar-currency').textContent =
    `${App.currency} ${App.user?.currency || 'INR'}`;
}

function showPage(pageId) {
  // For auth pages
  document.querySelectorAll('.auth-page').forEach(p => p.classList.remove('active'));
  const authPage = document.getElementById(pageId);
  if (authPage && authPage.classList.contains('auth-page')) {
    authPage.classList.add('active');
    return;
  }

  // For app pages
  const pageName = pageId.replace('-page', '');
  navigateTo(pageName);
}

function navigateTo(pageName) {
  // Update nav items
  document.querySelectorAll('.nav-item').forEach(item => {
    item.classList.toggle('active', item.dataset.page === pageName);
  });

  // Hide all pages, show target
  document.querySelectorAll('.content-area .page').forEach(p => p.classList.remove('active'));
  const target = document.getElementById(`${pageName}-page`);
  if (target) {
    target.classList.add('active');
    App.currentPage = pageName;
    document.getElementById('page-title').textContent = pageName.charAt(0).toUpperCase() + pageName.slice(1);
  }

  // Load page data
  const loaders = {
    dashboard: loadDashboard,
    transactions: loadTransactions,
    budgets: loadBudgets,
    savings: loadSavings,
    wallets: loadWallets,
    analytics: loadAnalytics,
    calendar: loadCalendar,
    reports: loadReports,
    settings: loadSettings,
  };
  if (loaders[pageName]) loaders[pageName]();

  // Close sidebar on mobile
  if (window.innerWidth < 768) closeSidebar();
}

// ==================== SIDEBAR ====================
function toggleSidebar() {
  const sidebar = document.getElementById('sidebar');
  const isOpen = sidebar.classList.contains('open');
  if (isOpen) closeSidebar();
  else openSidebar();
}

function openSidebar() {
  document.getElementById('sidebar').classList.add('open');
  let overlay = document.getElementById('sidebar-overlay');
  if (!overlay) {
    overlay = document.createElement('div');
    overlay.id = 'sidebar-overlay';
    overlay.className = 'sidebar-overlay';
    overlay.onclick = closeSidebar;
    document.body.appendChild(overlay);
  }
  overlay.classList.add('active');
}

function closeSidebar() {
  document.getElementById('sidebar').classList.remove('open');
  const overlay = document.getElementById('sidebar-overlay');
  if (overlay) overlay.classList.remove('active');
}

// ==================== DARK MODE ====================
function toggleDarkMode() {
  App.darkMode = !App.darkMode;
  applyDarkMode(App.darkMode);
  if (App.user) {
    API.put('/users/preferences', { darkMode: App.darkMode, currency: App.user.currency })
        .catch(() => {});
  }
}

function applyDarkMode(dark) {
  document.body.classList.toggle('dark-mode', dark);
  const icon = document.getElementById('theme-icon');
  if (icon) icon.className = dark ? 'fas fa-sun' : 'fas fa-moon';
}

// ==================== USER UI ====================
function updateUserUI() {
  if (!App.user) return;
  const name = App.user.fullName || 'User';
  const email = App.user.email || '';
  const initial = name.charAt(0).toUpperCase();

  const els = {
    'user-name-sidebar': name,
    'user-email-sidebar': email,
    'user-avatar-sidebar': initial,
    'user-avatar-top': initial,
  };
  Object.entries(els).forEach(([id, val]) => {
    const el = document.getElementById(id);
    if (el) el.textContent = val;
  });
}

// ==================== MODAL ====================
function openModal(title, bodyHtml, onConfirm = null) {
  document.getElementById('modal-title').textContent = title;
  document.getElementById('modal-body').innerHTML = bodyHtml;
  document.getElementById('modal-overlay').classList.remove('hidden');
  document.body.style.overflow = 'hidden';
}

function closeModal(e) {
  if (e && e.target !== document.getElementById('modal-overlay')) return;
  closeModalBtn();
}

function closeModalBtn() {
  document.getElementById('modal-overlay').classList.add('hidden');
  document.body.style.overflow = '';
}

// ==================== TOAST ====================
function showToast(message, type = 'info') {
  const icons = { success: 'check-circle', error: 'exclamation-circle', warning: 'exclamation-triangle', info: 'info-circle' };
  const toast = document.createElement('div');
  toast.className = `toast ${type}`;
  toast.innerHTML = `
    <i class="fas fa-${icons[type]} toast-icon"></i>
    <span class="toast-message">${message}</span>
    <button class="toast-close" onclick="this.parentElement.remove()"><i class="fas fa-times"></i></button>
  `;
  document.getElementById('toast-container').appendChild(toast);
  setTimeout(() => { if (toast.parentElement) toast.remove(); }, 4500);
}

// ==================== CURRENCY FORMAT ====================
function formatCurrency(amount, showSign = false) {
  const num = parseFloat(amount) || 0;
  const sign = showSign && num > 0 ? '+' : '';
  return `${sign}${App.currency}${Math.abs(num).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
}

function formatDate(dateStr) {
  if (!dateStr) return '';
  const d = new Date(dateStr);
  return d.toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' });
}

function todayISO() {
  return new Date().toISOString().split('T')[0];
}

// ==================== TOGGLE PASSWORD ====================
function togglePassword(inputId) {
  const input = document.getElementById(inputId);
  input.type = input.type === 'password' ? 'text' : 'password';
}

// ==================== LOADING ====================
function showLoading() { document.getElementById('loading-overlay').classList.remove('hidden'); }
function hideLoading() { document.getElementById('loading-overlay').classList.add('hidden'); }

// ==================== FAB ====================
function openAddTransaction() { openTransactionModal(); }

// ==================== PWA ====================
let deferredPrompt = null;
window.addEventListener('beforeinstallprompt', (e) => {
  e.preventDefault();
  deferredPrompt = e;
  document.getElementById('install-btn').style.display = 'flex';
});

function installPwa() {
  if (!deferredPrompt) return;
  deferredPrompt.prompt();
  deferredPrompt.userChoice.then(() => {
    deferredPrompt = null;
    document.getElementById('install-btn').style.display = 'none';
  });
}

// ==================== INIT ====================
document.addEventListener('DOMContentLoaded', () => {
  // Check for password reset token in URL
  const urlParams = new URLSearchParams(window.location.search);
  const resetToken = urlParams.get('reset');
  if (resetToken) {
    window.pendingResetToken = resetToken;
    showAuthSection();
    showPage('reset-page');
    return;
  }

  if (isLoggedIn() && App.user) {
    showAppSection();
    navigateTo('dashboard');
  } else {
    showAuthSection();
    showPage('login-page');
  }

  // Nav click handlers
  document.querySelectorAll('.nav-item').forEach(item => {
    item.addEventListener('click', (e) => {
      e.preventDefault();
      navigateTo(item.dataset.page);
    });
  });

  // Dark mode from localStorage
  const savedMode = localStorage.getItem('bt_dark');
  if (savedMode !== null) {
    App.darkMode = savedMode === 'true';
    applyDarkMode(App.darkMode);
  }
});

window.addEventListener('storage', (e) => {
  if (e.key === 'bt_token' && !e.newValue) logout();
});
