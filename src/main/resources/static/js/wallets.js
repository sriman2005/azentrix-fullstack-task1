/* Wallets JS */
async function loadWallets() {
  const page = document.getElementById('wallets-page');
  page.innerHTML = `<div class="page-header"><h2>Wallets</h2></div><div class="skeleton" style="height:300px;border-radius:12px"></div>`;
  try {
    const res = await API.get('/wallets');
    renderWallets(res.data || []);
  } catch(err) { page.innerHTML = `<div class="empty-state"><p>Failed: ${err.message}</p></div>`; }
}

function renderWallets(wallets) {
  const page = document.getElementById('wallets-page');
  const totalBalance = wallets.reduce((s,w) => s + parseFloat(w.balance||0), 0);
  page.innerHTML = `
    <div class="page-header">
      <h2>Wallets & Accounts 💳</h2>
      <div class="page-header-actions">
        <button class="btn-secondary" onclick="openTransferModal()"><i class="fas fa-exchange-alt"></i> Transfer</button>
        <button class="btn-primary" onclick="openWalletModal()"><i class="fas fa-plus"></i> Add Wallet</button>
      </div>
    </div>
    <div class="card" style="margin-bottom:24px;text-align:center">
      <div style="font-size:13px;color:var(--text-muted);margin-bottom:4px">Total Balance (All Accounts)</div>
      <div style="font-size:36px;font-weight:800;color:${totalBalance>=0?'var(--income-color)':'var(--expense-color)'}">${formatCurrency(totalBalance)}</div>
    </div>
    ${wallets.length === 0 ? `<div class="empty-state"><div class="empty-state-icon">💳</div><h3>No wallets</h3><p>Add your bank accounts, cash wallets, etc.</p></div>` :
    `<div class="wallet-grid">${wallets.map(w => walletCard(w)).join('')}</div>`}
  `;
}

function walletCard(w) {
  const GRADIENTS = {
    CASH: 'linear-gradient(135deg,#6366f1,#8b5cf6)',
    BANK: 'linear-gradient(135deg,#0ea5e9,#06b6d4)',
    CREDIT_CARD: 'linear-gradient(135deg,#ef4444,#f97316)',
    SAVINGS: 'linear-gradient(135deg,#10b981,#059669)',
    INVESTMENT: 'linear-gradient(135deg,#f59e0b,#d97706)',
  };
  const grad = GRADIENTS[w.type] || GRADIENTS.CASH;
  return `
    <div class="wallet-card" style="background:${grad}">
      <div class="wallet-chip">
        <div class="wallet-type">${w.type.replace('_',' ')} ${w.isDefault?'• Default':''}</div>
        <div class="wallet-icon"><i class="fas fa-${w.icon||'wallet'}"></i></div>
      </div>
      <div class="wallet-balance">${parseFloat(w.balance) < 0 ? '-' : ''}${formatCurrency(Math.abs(parseFloat(w.balance)))}</div>
      <div class="wallet-name">${escHtml(w.name)}</div>
      <div class="wallet-actions">
        <button class="wallet-action-btn" onclick="openWalletModal(${JSON.stringify(w).replace(/"/g,'&quot;')})"><i class="fas fa-edit"></i> Edit</button>
        ${!w.isDefault?`<button class="wallet-action-btn" onclick="deleteWallet(${w.id})"><i class="fas fa-trash"></i></button>`:''}
      </div>
    </div>
  `;
}

function openWalletModal(w = null) {
  const ICONS = ['wallet','credit-card','university','piggy-bank','coins','chart-line','briefcase'];
  const TYPES = ['CASH','BANK','CREDIT_CARD','SAVINGS','INVESTMENT'];
  openModal(w ? 'Edit Wallet' : 'Add Wallet', `
    <div class="form-group">
      <label>Wallet Name *</label>
      <input type="text" id="w-name" placeholder="e.g. SBI Savings" value="${escHtml(w?.name||'')}"/>
    </div>
    <div class="form-group">
      <label>Type</label>
      <select id="w-type">
        ${TYPES.map(t => `<option value="${t}" ${w?.type===t?'selected':''}>${t.replace('_',' ')}</option>`).join('')}
      </select>
    </div>
    ${!w ? `<div class="form-group">
      <label>Opening Balance (${App.currency})</label>
      <input type="number" id="w-balance" step="0.01" value="0" min="0"/>
    </div>` : ''}
    <div class="form-group">
      <label>Icon</label>
      <div style="display:flex;gap:8px;flex-wrap:wrap;margin-top:6px">
        ${ICONS.map(ic => `<button type="button" onclick="selectWalletIcon('${ic}')" id="wicon-${ic}" style="width:36px;height:36px;border-radius:8px;background:${(w?.icon||'wallet')===ic?'var(--gradient-brand)':'var(--bg-tertiary)'};color:${(w?.icon||'wallet')===ic?'white':'var(--text-secondary)'};border:1px solid var(--border);transition:all 0.2s">
          <i class="fas fa-${ic}"></i></button>`).join('')}
      </div>
      <input type="hidden" id="w-icon" value="${w?.icon||'wallet'}"/>
    </div>
    <div class="form-group" style="display:flex;align-items:center;gap:10px">
      <input type="checkbox" id="w-default" ${w?.isDefault?'checked':''} style="width:16px;height:16px;accent-color:var(--accent)"/>
      <label for="w-default" style="margin:0;cursor:pointer">Set as default wallet</label>
    </div>
    <div class="modal-actions">
      <button class="btn-secondary" onclick="closeModalBtn()">Cancel</button>
      <button class="btn-primary" onclick="saveWallet(${w?.id||'null'})"><i class="fas fa-check"></i> ${w?'Update':'Add'}</button>
    </div>
  `);
}

function selectWalletIcon(icon) {
  document.getElementById('w-icon').value = icon;
  document.querySelectorAll('[id^="wicon-"]').forEach(btn => {
    const sel = btn.id === `wicon-${icon}`;
    btn.style.background = sel ? 'var(--gradient-brand)' : 'var(--bg-tertiary)';
    btn.style.color = sel ? 'white' : 'var(--text-secondary)';
  });
}

async function saveWallet(existingId) {
  const name = document.getElementById('w-name').value.trim();
  if (!name) { showToast('Please enter a wallet name', 'error'); return; }
  try {
    showLoading();
    const body = {
      name, type: document.getElementById('w-type').value,
      icon: document.getElementById('w-icon').value,
      isDefault: document.getElementById('w-default').checked,
    };
    if (!existingId && document.getElementById('w-balance'))
      body.balance = parseFloat(document.getElementById('w-balance').value||0);
    if (existingId) await API.put(`/wallets/${existingId}`, body);
    else await API.post('/wallets', body);
    closeModalBtn();
    showToast(`Wallet ${existingId?'updated':'added'}!`, 'success');
    loadWallets();
  } catch(err) { showToast(err.message, 'error'); } finally { hideLoading(); }
}

async function deleteWallet(id) {
  if (!confirm('Delete this wallet? All associated transactions will remain.')) return;
  try {
    showLoading();
    await API.delete(`/wallets/${id}`);
    showToast('Wallet deleted', 'info');
    loadWallets();
  } catch(err) { showToast(err.message, 'error'); } finally { hideLoading(); }
}

async function openTransferModal() {
  const res = await API.get('/wallets');
  const wallets = res.data || [];
  const opts = wallets.map(w => `<option value="${w.id}">${w.name} (${formatCurrency(w.balance)})</option>`).join('');
  openModal('Transfer Between Wallets', `
    <div class="form-group"><label>From Wallet</label><select id="tf-from">${opts}</select></div>
    <div class="form-group"><label>To Wallet</label><select id="tf-to">${opts}</select></div>
    <div class="form-group"><label>Amount (${App.currency}) *</label><input type="number" id="tf-amount" step="0.01" min="0.01" placeholder="0.00"/></div>
    <div class="form-group"><label>Date</label><input type="date" id="tf-date" value="${todayISO()}"/></div>
    <div class="modal-actions">
      <button class="btn-secondary" onclick="closeModalBtn()">Cancel</button>
      <button class="btn-primary" onclick="doTransfer()"><i class="fas fa-exchange-alt"></i> Transfer</button>
    </div>
  `);
}

async function doTransfer() {
  const fromId = parseInt(document.getElementById('tf-from').value);
  const toId = parseInt(document.getElementById('tf-to').value);
  const amount = parseFloat(document.getElementById('tf-amount').value);
  if (fromId === toId) { showToast('Please select different wallets', 'error'); return; }
  if (!amount || amount <= 0) { showToast('Please enter a valid amount', 'error'); return; }
  try {
    showLoading();
    await API.post('/wallets/transfer', { fromWalletId: fromId, toWalletId: toId, amount, date: document.getElementById('tf-date').value });
    closeModalBtn();
    showToast('Transfer successful! 💸', 'success');
    loadWallets();
  } catch(err) { showToast(err.message, 'error'); } finally { hideLoading(); }
}
