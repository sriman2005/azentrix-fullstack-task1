/* Admin Panel JS */
async function loadAdmin() {
  const page = document.getElementById('admin-page');
  if (!App.user || App.user.role !== 'ADMIN') {
    page.innerHTML = `<div class="empty-state"><div class="empty-state-icon">🔒</div><h3>Access Denied</h3><p>Admin privileges required.</p></div>`;
    return;
  }
  page.innerHTML = `<div class="page-header"><h2>Admin Panel 🛡️</h2></div><div class="skeleton" style="height:400px;border-radius:12px"></div>`;
  try {
    const [statsRes, usersRes] = await Promise.all([API.get('/admin/stats'), API.get('/admin/users')]);
    renderAdminPanel(statsRes.data, usersRes.data || []);
  } catch (err) {
    page.innerHTML = `<div class="empty-state"><div class="empty-state-icon">⚠️</div><h3>Failed to load</h3><p>${err.message}</p></div>`;
  }
}

function renderAdminPanel(stats, users) {
  const page = document.getElementById('admin-page');
  page.innerHTML = `
    <div class="page-header">
      <h2>Admin Panel 🛡️</h2>
    </div>

    <!-- Stats Cards -->
    <div class="summary-grid" style="margin-bottom:24px">
      <div class="summary-card">
        <div class="summary-icon" style="background:linear-gradient(135deg,#6366f1,#8b5cf6)"><i class="fas fa-users"></i></div>
        <div class="summary-info"><div class="summary-value">${stats.totalUsers}</div><div class="summary-label">Total Users</div></div>
      </div>
      <div class="summary-card">
        <div class="summary-icon" style="background:linear-gradient(135deg,#10b981,#059669)"><i class="fas fa-exchange-alt"></i></div>
        <div class="summary-info"><div class="summary-value">${stats.totalTransactions}</div><div class="summary-label">Transactions</div></div>
      </div>
      <div class="summary-card">
        <div class="summary-icon" style="background:linear-gradient(135deg,#f59e0b,#d97706)"><i class="fas fa-wallet"></i></div>
        <div class="summary-info"><div class="summary-value">${stats.totalWallets}</div><div class="summary-label">Wallets</div></div>
      </div>
      <div class="summary-card">
        <div class="summary-icon" style="background:linear-gradient(135deg,#ef4444,#dc2626)"><i class="fas fa-piggy-bank"></i></div>
        <div class="summary-info"><div class="summary-value">${stats.totalSavingsGoals}</div><div class="summary-label">Savings Goals</div></div>
      </div>
    </div>

    <!-- User Management Table -->
    <div class="card">
      <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px">
        <h3 style="margin:0"><i class="fas fa-users-cog" style="margin-right:8px;color:var(--accent)"></i>User Management</h3>
        <span style="color:var(--text-muted);font-size:13px">${users.length} users</span>
      </div>
      <div style="overflow-x:auto">
        <table class="data-table" style="width:100%;border-collapse:collapse">
          <thead>
            <tr style="border-bottom:2px solid var(--border)">
              <th style="text-align:left;padding:12px 8px;font-size:12px;text-transform:uppercase;color:var(--text-muted)">User</th>
              <th style="text-align:left;padding:12px 8px;font-size:12px;text-transform:uppercase;color:var(--text-muted)">Email</th>
              <th style="text-align:center;padding:12px 8px;font-size:12px;text-transform:uppercase;color:var(--text-muted)">Role</th>
              <th style="text-align:center;padding:12px 8px;font-size:12px;text-transform:uppercase;color:var(--text-muted)">Transactions</th>
              <th style="text-align:right;padding:12px 8px;font-size:12px;text-transform:uppercase;color:var(--text-muted)">Balance</th>
              <th style="text-align:center;padding:12px 8px;font-size:12px;text-transform:uppercase;color:var(--text-muted)">Joined</th>
              <th style="text-align:center;padding:12px 8px;font-size:12px;text-transform:uppercase;color:var(--text-muted)">Actions</th>
            </tr>
          </thead>
          <tbody>
            ${users.map(u => `
              <tr style="border-bottom:1px solid var(--border)">
                <td style="padding:12px 8px">
                  <div style="display:flex;align-items:center;gap:10px">
                    <div style="width:36px;height:36px;border-radius:50%;background:var(--gradient-brand);display:flex;align-items:center;justify-content:center;color:white;font-weight:600;font-size:14px">${(u.fullName || 'U').charAt(0).toUpperCase()}</div>
                    <span style="font-weight:500">${escHtml(u.fullName)}</span>
                  </div>
                </td>
                <td style="padding:12px 8px;color:var(--text-muted);font-size:13px">${escHtml(u.email)}</td>
                <td style="padding:12px 8px;text-align:center">
                  <span class="badge ${u.role === 'ADMIN' ? 'badge-admin' : 'badge-user'}">${u.role}</span>
                </td>
                <td style="padding:12px 8px;text-align:center;font-weight:500">${u.transactionCount}</td>
                <td style="padding:12px 8px;text-align:right;font-weight:600;color:${parseFloat(u.totalBalance) >= 0 ? 'var(--income-color)' : 'var(--expense-color)'}">${formatCurrency(u.totalBalance)}</td>
                <td style="padding:12px 8px;text-align:center;color:var(--text-muted);font-size:13px">${u.createdAt ? new Date(u.createdAt).toLocaleDateString('en-IN', {day:'2-digit',month:'short',year:'numeric'}) : '-'}</td>
                <td style="padding:12px 8px;text-align:center">
                  <div style="display:flex;gap:6px;justify-content:center">
                    <button class="icon-btn-sm" title="Toggle Role" onclick="toggleUserRole(${u.id}, '${u.role}')" ${u.role === 'ADMIN' ? 'disabled style="opacity:0.4"' : ''}>
                      <i class="fas fa-user-shield"></i>
                    </button>
                    <button class="icon-btn-sm danger" title="Delete User" onclick="deleteUserAdmin(${u.id}, '${escHtml(u.fullName)}')" ${u.role === 'ADMIN' ? 'disabled style="opacity:0.4"' : ''}>
                      <i class="fas fa-trash"></i>
                    </button>
                  </div>
                </td>
              </tr>
            `).join('')}
          </tbody>
        </table>
      </div>
    </div>
  `;
}

async function toggleUserRole(userId, currentRole) {
  const newRole = currentRole === 'ADMIN' ? 'USER' : 'ADMIN';
  if (!confirm(`Change this user's role to ${newRole}?`)) return;
  try {
    showLoading();
    await API.put(`/admin/users/${userId}/role`, { role: newRole });
    showToast(`User role changed to ${newRole}`, 'success');
    loadAdmin();
  } catch (err) { showToast(err.message, 'error'); } finally { hideLoading(); }
}

async function deleteUserAdmin(userId, name) {
  if (!confirm(`⚠️ DELETE user "${name}"? This will remove ALL their data permanently.`)) return;
  try {
    showLoading();
    await API.delete(`/admin/users/${userId}`);
    showToast(`User "${name}" deleted`, 'info');
    loadAdmin();
  } catch (err) { showToast(err.message, 'error'); } finally { hideLoading(); }
}

function escHtml(s) { const d = document.createElement('div'); d.textContent = s || ''; return d.innerHTML; }
