/* Settings JS */
async function loadSettings() {
  const page = document.getElementById('settings-page');
  page.innerHTML = `<div class="page-header"><h2>Settings</h2></div><div class="skeleton" style="height:600px;border-radius:12px"></div>`;
  try {
    const [profileRes, recurringRes, catsRes] = await Promise.all([
      API.get('/users/profile'),
      API.get('/recurring'),
      API.get('/categories')
    ]);
    renderSettings(profileRes.data, recurringRes.data || [], catsRes.data || []);
  } catch(err) { page.innerHTML = `<div class="empty-state"><p>Failed to load settings: ${err.message}</p></div>`; }
}

function renderSettings(profile, recurring, categories) {
  const page = document.getElementById('settings-page');
  const CURRENCIES = ['INR','USD','EUR','GBP','JPY','AUD','CAD','SGD'];
  page.innerHTML = `
    <div class="page-header"><h2>Settings ⚙️</h2></div>

    <!-- Profile -->
    <div class="card" style="margin-bottom:16px">
      <div class="card-title"><span><i class="fas fa-user" style="color:var(--accent)"></i> Profile</span></div>
      <div style="display:grid;grid-template-columns:1fr 1fr;gap:16px">
        <div class="form-group" style="margin:0">
          <label style="font-size:13px;color:var(--text-secondary);display:block;margin-bottom:6px">Full Name</label>
          <input type="text" id="prof-name" value="${escHtml(profile.fullName||'')}" style="width:100%;padding:11px 14px;background:var(--bg-tertiary);border:1.5px solid var(--border);border-radius:10px;color:var(--text-primary)"/>
        </div>
        <div class="form-group" style="margin:0">
          <label style="font-size:13px;color:var(--text-secondary);display:block;margin-bottom:6px">Email (cannot change)</label>
          <input type="email" value="${escHtml(profile.email||'')}" disabled style="width:100%;padding:11px 14px;background:var(--bg-tertiary);border:1.5px solid var(--border);border-radius:10px;color:var(--text-muted);opacity:0.7"/>
        </div>
        <div class="form-group" style="margin:0">
          <label style="font-size:13px;color:var(--text-secondary);display:block;margin-bottom:6px">Phone</label>
          <input type="tel" id="prof-phone" value="${escHtml(profile.phone||'')}" style="width:100%;padding:11px 14px;background:var(--bg-tertiary);border:1.5px solid var(--border);border-radius:10px;color:var(--text-primary)"/>
        </div>
        <div class="form-group" style="margin:0">
          <label style="font-size:13px;color:var(--text-secondary);display:block;margin-bottom:6px">Currency</label>
          <select id="prof-currency" style="width:100%;padding:11px 14px;background:var(--bg-tertiary);border:1.5px solid var(--border);border-radius:10px;color:var(--text-primary)">
            ${CURRENCIES.map(c => `<option value="${c}" ${profile.currency===c?'selected':''}>${c}</option>`).join('')}
          </select>
        </div>
      </div>
      <button class="btn-primary" style="margin-top:16px" onclick="saveProfile()">
        <i class="fas fa-save"></i> Save Profile
      </button>
    </div>

    <!-- Appearance -->
    <div class="card" style="margin-bottom:16px">
      <div class="card-title"><span><i class="fas fa-palette" style="color:var(--accent)"></i> Appearance</span></div>
      <div style="display:flex;align-items:center;justify-content:space-between;padding:14px 0;border-bottom:1px solid var(--border)">
        <div>
          <div style="font-weight:500;color:var(--text-primary)">Dark Mode</div>
          <div style="font-size:13px;color:var(--text-muted)">Toggle between dark and light theme</div>
        </div>
        <label class="toggle-switch">
          <input type="checkbox" id="dark-mode-toggle" ${document.body.classList.contains('dark-mode')?'checked':''} onchange="toggleDarkMode()"/>
          <span class="toggle-slider"></span>
        </label>
      </div>
    </div>

    <!-- Security -->
    <div class="card" style="margin-bottom:16px">
      <div class="card-title"><span><i class="fas fa-shield-alt" style="color:var(--accent)"></i> Security</span></div>
      <div class="form-group">
        <label style="font-size:13px;color:var(--text-secondary);display:block;margin-bottom:6px">Current Password</label>
        <input type="password" id="curr-pwd" placeholder="••••••••" style="width:100%;padding:11px 14px;background:var(--bg-tertiary);border:1.5px solid var(--border);border-radius:10px;color:var(--text-primary)"/>
      </div>
      <div class="form-group">
        <label style="font-size:13px;color:var(--text-secondary);display:block;margin-bottom:6px">New Password (min 8 chars)</label>
        <input type="password" id="new-pwd" placeholder="••••••••" style="width:100%;padding:11px 14px;background:var(--bg-tertiary);border:1.5px solid var(--border);border-radius:10px;color:var(--text-primary)"/>
      </div>
      <button class="btn-primary" onclick="changePassword()">
        <i class="fas fa-key"></i> Change Password
      </button>
    </div>

    <!-- Categories -->
    <div class="card" style="margin-bottom:16px">
      <div class="card-title">
        <span><i class="fas fa-tags" style="color:var(--accent)"></i> Categories</span>
        <button class="btn-primary" style="padding:7px 14px;font-size:13px" onclick="openCatModal()">
          <i class="fas fa-plus"></i> Add
        </button>
      </div>
      <div id="cat-list">
        ${categories.filter(c=>!c.default).map(c => `
          <div style="display:flex;align-items:center;gap:10px;padding:10px 0;border-bottom:1px solid var(--border)">
            <div style="width:32px;height:32px;border-radius:8px;background:${c.color}22;color:${c.color};display:flex;align-items:center;justify-content:center">
              <i class="fas fa-${c.icon}"></i>
            </div>
            <span style="flex:1;font-size:14px;font-weight:500">${escHtml(c.name)}</span>
            <span class="badge ${c.type==='INCOME'?'income':'expense'}">${c.type}</span>
            <button class="tx-action-btn del" onclick="deleteCategory(${c.id})"><i class="fas fa-trash"></i></button>
          </div>
        `).join('') || '<p style="color:var(--text-muted);font-size:14px;padding:12px 0">No custom categories. Default categories are pre-loaded.</p>'}
      </div>
    </div>

    <!-- Recurring Transactions -->
    <div class="card" style="margin-bottom:16px">
      <div class="card-title">
        <span><i class="fas fa-repeat" style="color:var(--accent)"></i> Recurring Transactions</span>
        <button class="btn-primary" style="padding:7px 14px;font-size:13px" onclick="openRecurringModal()">
          <i class="fas fa-plus"></i> Add
        </button>
      </div>
      <div id="recurring-list">
        ${recurring.length === 0 ? '<p style="color:var(--text-muted);font-size:14px;padding:12px 0">No recurring transactions set up.</p>' :
          recurring.map(r => `
            <div style="display:flex;align-items:center;gap:12px;padding:12px 0;border-bottom:1px solid var(--border)">
              <div style="flex:1">
                <div style="font-size:14px;font-weight:600;color:var(--text-primary)">${escHtml(r.description||r.categoryName||'Recurring')}</div>
                <div style="font-size:12px;color:var(--text-muted)">${r.frequency} • ${r.type} • Next: ${formatDate(r.nextExecution)}</div>
              </div>
              <span class="badge ${r.type==='INCOME'?'income':'expense'}">${formatCurrency(r.amount)}</span>
              <span class="badge ${r.active?'active':'inactive'}">${r.active?'Active':'Paused'}</span>
              <button class="tx-action-btn edit" onclick="toggleRecurring(${r.id})" title="Toggle active"><i class="fas fa-${r.active?'pause':'play'}"></i></button>
              <button class="tx-action-btn del" onclick="deleteRecurring(${r.id})"><i class="fas fa-trash"></i></button>
            </div>
          `).join('')}
      </div>
    </div>

    <!-- Account -->
    <div class="card">
      <div class="card-title"><span><i class="fas fa-user-circle" style="color:var(--accent)"></i> Account</span></div>
      <div style="display:flex;align-items:center;justify-content:space-between;padding:14px 0">
        <div>
          <div style="font-weight:500">Sign Out</div>
          <div style="font-size:13px;color:var(--text-muted)">Sign out of your account on this device</div>
        </div>
        <button class="btn-danger" onclick="logout()"><i class="fas fa-sign-out-alt"></i> Sign Out</button>
      </div>
    </div>
  `;

  // Add toggle switch CSS if not exists
  if (!document.getElementById('toggle-style')) {
    const style = document.createElement('style');
    style.id = 'toggle-style';
    style.textContent = `.toggle-switch{position:relative;display:inline-block;width:48px;height:26px}.toggle-switch input{opacity:0;width:0;height:0}.toggle-slider{position:absolute;cursor:pointer;inset:0;background:var(--border);border-radius:26px;transition:.3s}.toggle-slider:before{content:"";position:absolute;width:20px;height:20px;left:3px;bottom:3px;background:white;border-radius:50%;transition:.3s}.toggle-switch input:checked+.toggle-slider{background:var(--accent)}.toggle-switch input:checked+.toggle-slider:before{transform:translateX(22px)}`;
    document.head.appendChild(style);
  }
}

async function saveProfile() {
  const updates = {
    fullName: document.getElementById('prof-name').value.trim(),
    phone: document.getElementById('prof-phone').value.trim(),
    currency: document.getElementById('prof-currency').value,
  };
  try {
    showLoading();
    await API.put('/users/profile', updates);
    await API.put('/users/preferences', { darkMode: App.darkMode, currency: updates.currency });
    App.user = { ...App.user, ...updates };
    App.currency = { INR:'₹',USD:'$',EUR:'€',GBP:'£',JPY:'¥',AUD:'A$',CAD:'C$',SGD:'S$' }[updates.currency] || '₹';
    localStorage.setItem('bt_user', JSON.stringify(App.user));
    document.getElementById('topbar-currency').textContent = `${App.currency} ${updates.currency}`;
    updateUserUI();
    showToast('Profile saved! ✅', 'success');
  } catch(err) { showToast(err.message, 'error'); } finally { hideLoading(); }
}

async function changePassword() {
  const curr = document.getElementById('curr-pwd').value;
  const newPwd = document.getElementById('new-pwd').value;
  if (!curr || !newPwd) { showToast('Please fill in both password fields', 'error'); return; }
  if (newPwd.length < 8) { showToast('New password must be at least 8 characters', 'error'); return; }
  try {
    showLoading();
    await API.put('/users/change-password', { currentPassword: curr, newPassword: newPwd });
    document.getElementById('curr-pwd').value = '';
    document.getElementById('new-pwd').value = '';
    showToast('Password changed successfully! 🔐', 'success');
  } catch(err) { showToast(err.message, 'error'); } finally { hideLoading(); }
}

function openCatModal() {
  openModal('Add Category', `
    <div class="form-group"><label>Name *</label><input type="text" id="cat-name" placeholder="e.g. Gym"/></div>
    <div class="form-group"><label>Type *</label>
      <select id="cat-type">
        <option value="EXPENSE">Expense</option>
        <option value="INCOME">Income</option>
      </select>
    </div>
    <div class="form-group"><label>Color</label>
      <input type="color" id="cat-color" value="#6366f1" style="width:100%;height:44px;border-radius:10px;border:1.5px solid var(--border);padding:4px;background:var(--bg-tertiary)"/>
    </div>
    <div class="modal-actions">
      <button class="btn-secondary" onclick="closeModalBtn()">Cancel</button>
      <button class="btn-primary" onclick="saveCategory()"><i class="fas fa-check"></i> Save</button>
    </div>
  `);
}

async function saveCategory() {
  const name = document.getElementById('cat-name').value.trim();
  if (!name) { showToast('Please enter a category name', 'error'); return; }
  try {
    showLoading();
    await API.post('/categories', { name, type: document.getElementById('cat-type').value, color: document.getElementById('cat-color').value, icon: 'tag' });
    closeModalBtn();
    showToast('Category added!', 'success');
    loadSettings();
  } catch(err) { showToast(err.message, 'error'); } finally { hideLoading(); }
}

async function deleteCategory(id) {
  if (!confirm('Delete this category?')) return;
  try {
    showLoading();
    await API.delete(`/categories/${id}`);
    showToast('Category deleted', 'info');
    loadSettings();
  } catch(err) { showToast(err.message, 'error'); } finally { hideLoading(); }
}

function openRecurringModal() {
  openModal('Add Recurring Transaction', `
    <div class="type-toggle">
      <button type="button" class="type-btn expense active" id="btn-rec-expense" onclick="document.getElementById('rec-type').value='EXPENSE';this.classList.add('active');document.getElementById('btn-rec-income').classList.remove('active')">
        <i class="fas fa-arrow-up"></i> Expense
      </button>
      <button type="button" class="type-btn income" id="btn-rec-income" onclick="document.getElementById('rec-type').value='INCOME';this.classList.add('active');document.getElementById('btn-rec-expense').classList.remove('active')">
        <i class="fas fa-arrow-down"></i> Income
      </button>
    </div>
    <input type="hidden" id="rec-type" value="EXPENSE"/>
    <div class="form-group"><label>Amount *</label><input type="number" id="rec-amount" step="0.01" min="0.01" placeholder="0.00"/></div>
    <div class="form-group"><label>Description</label><input type="text" id="rec-desc" placeholder="e.g. Netflix Subscription"/></div>
    <div class="form-group"><label>Frequency</label>
      <select id="rec-freq">
        <option value="DAILY">Daily</option>
        <option value="WEEKLY">Weekly</option>
        <option value="MONTHLY" selected>Monthly</option>
        <option value="YEARLY">Yearly</option>
      </select>
    </div>
    <div class="form-group"><label>Start Date *</label><input type="date" id="rec-start" value="${todayISO()}"/></div>
    <div class="form-group"><label>End Date (optional)</label><input type="date" id="rec-end"/></div>
    <div class="modal-actions">
      <button class="btn-secondary" onclick="closeModalBtn()">Cancel</button>
      <button class="btn-primary" onclick="saveRecurring()"><i class="fas fa-check"></i> Save</button>
    </div>
  `);
}

async function saveRecurring() {
  const amount = parseFloat(document.getElementById('rec-amount').value);
  const start = document.getElementById('rec-start').value;
  if (!amount || amount <= 0) { showToast('Please enter a valid amount', 'error'); return; }
  if (!start) { showToast('Please select a start date', 'error'); return; }
  try {
    showLoading();
    await API.post('/recurring', {
      type: document.getElementById('rec-type').value,
      amount, description: document.getElementById('rec-desc').value,
      frequency: document.getElementById('rec-freq').value,
      startDate: start,
      endDate: document.getElementById('rec-end').value || null,
    });
    closeModalBtn();
    showToast('Recurring transaction added! 🔄', 'success');
    loadSettings();
  } catch(err) { showToast(err.message, 'error'); } finally { hideLoading(); }
}

async function toggleRecurring(id) {
  try {
    showLoading();
    await API.patch(`/recurring/${id}/toggle`);
    showToast('Updated!', 'success');
    loadSettings();
  } catch(err) { showToast(err.message, 'error'); } finally { hideLoading(); }
}

async function deleteRecurring(id) {
  if (!confirm('Delete this recurring transaction?')) return;
  try {
    showLoading();
    await API.delete(`/recurring/${id}`);
    showToast('Deleted', 'info');
    loadSettings();
  } catch(err) { showToast(err.message, 'error'); } finally { hideLoading(); }
}
