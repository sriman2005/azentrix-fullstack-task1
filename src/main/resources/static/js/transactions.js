/* =============================================
   Transactions JS — CRUD, Search, Filter, Voice
   ============================================= */

let txPage = 0, txSize = 15, txTotal = 0;
let txFilters = { type: '', categoryId: '', walletId: '', startDate: '', endDate: '', search: '' };
let allCategories = [], allWallets = [];
let editingTxId = null;
let isVoiceListening = false;

async function loadTransactions() {
  const page = document.getElementById('transactions-page');
  page.innerHTML = buildTxSkeleton();

  try {
    const [catsRes, walletsRes] = await Promise.all([API.get('/categories'), API.get('/wallets')]);
    allCategories = catsRes.data || [];
    allWallets = walletsRes.data || [];
    page.innerHTML = buildTxPage();
    await fetchTransactions();
  } catch (err) {
    showToast('Failed to load transactions: ' + err.message, 'error');
  }
}

function buildTxSkeleton() {
  return `<div class="page-header"><h2>Transactions</h2></div><div class="skeleton" style="height:56px;margin-bottom:12px"></div><div class="skeleton" style="height:400px"></div>`;
}

function buildTxPage() {
  const catOptions = allCategories.map(c => `<option value="${c.id}">${c.name}</option>`).join('');
  const walletOptions = allWallets.map(w => `<option value="${w.id}">${w.name}</option>`).join('');
  return `
    <div class="page-header">
      <h2>Transactions 💳</h2>
      <div class="page-header-actions">
        <button class="voice-btn" id="voice-btn" onclick="startVoiceEntry()">
          <i class="fas fa-microphone"></i> Voice Entry
        </button>
        <button class="btn-primary" onclick="openTransactionModal()">
          <i class="fas fa-plus"></i> Add
        </button>
      </div>
    </div>

    <!-- Search -->
    <div class="search-bar">
      <i class="fas fa-search"></i>
      <input type="text" id="tx-search" placeholder="Search transactions..." oninput="debouncedSearch(this.value)"/>
      <button onclick="clearSearch()" style="background:none;color:var(--text-muted)" title="Clear"><i class="fas fa-times"></i></button>
    </div>

    <!-- Filters -->
    <div class="filter-row">
      <select class="filter-select" id="filter-type" onchange="applyFilter('type', this.value)">
        <option value="">All Types</option>
        <option value="INCOME">Income</option>
        <option value="EXPENSE">Expense</option>
      </select>
      <select class="filter-select" id="filter-cat" onchange="applyFilter('categoryId', this.value)">
        <option value="">All Categories</option>
        ${catOptions}
      </select>
      <select class="filter-select" id="filter-wallet" onchange="applyFilter('walletId', this.value)">
        <option value="">All Wallets</option>
        ${walletOptions}
      </select>
      <input type="date" class="filter-select" id="filter-start" onchange="applyFilter('startDate', this.value)" title="Start Date"/>
      <input type="date" class="filter-select" id="filter-end" onchange="applyFilter('endDate', this.value)" title="End Date"/>
      <button class="btn-secondary" style="padding:8px 16px;font-size:13px" onclick="clearFilters()">
        <i class="fas fa-times"></i> Clear
      </button>
    </div>

    <div class="card">
      <div class="card-title">
        <span>Transactions <span id="tx-count" style="font-size:13px;color:var(--text-muted);font-weight:400"></span></span>
        <div style="display:flex;gap:8px">
          <select class="filter-select" id="tx-size-select" onchange="changeTxSize(this.value)" style="font-size:12px">
            <option value="15">15/page</option>
            <option value="30">30/page</option>
            <option value="50">50/page</option>
          </select>
        </div>
      </div>
      <div id="tx-list" class="transaction-list"></div>
      <div id="tx-pagination" class="pagination"></div>
    </div>
  `;
}

let searchTimer = null;
function debouncedSearch(val) {
  clearTimeout(searchTimer);
  searchTimer = setTimeout(() => { txFilters.search = val; txPage = 0; fetchTransactions(); }, 400);
}

function clearSearch() {
  document.getElementById('tx-search').value = '';
  txFilters.search = '';
  txPage = 0;
  fetchTransactions();
}

function applyFilter(key, value) {
  txFilters[key] = value;
  txPage = 0;
  fetchTransactions();
}

function clearFilters() {
  txFilters = { type: '', categoryId: '', walletId: '', startDate: '', endDate: '', search: '' };
  ['filter-type','filter-cat','filter-wallet','filter-start','filter-end'].forEach(id => {
    const el = document.getElementById(id);
    if (el) el.value = '';
  });
  const search = document.getElementById('tx-search');
  if (search) search.value = '';
  txPage = 0;
  fetchTransactions();
}

function changeTxSize(size) {
  txSize = parseInt(size);
  txPage = 0;
  fetchTransactions();
}

async function fetchTransactions() {
  const list = document.getElementById('tx-list');
  if (!list) return;
  list.innerHTML = `<div class="skeleton" style="height:320px;border-radius:12px"></div>`;

  const params = new URLSearchParams({ page: txPage, size: txSize });
  if (txFilters.type) params.set('type', txFilters.type);
  if (txFilters.categoryId) params.set('categoryId', txFilters.categoryId);
  if (txFilters.walletId) params.set('walletId', txFilters.walletId);
  if (txFilters.startDate) params.set('startDate', txFilters.startDate);
  if (txFilters.endDate) params.set('endDate', txFilters.endDate);
  if (txFilters.search) params.set('search', txFilters.search);

  try {
    const res = await API.get(`/transactions?${params}`);
    const data = res.data;
    const txns = data.content || [];
    txTotal = data.totalElements || 0;

    const countEl = document.getElementById('tx-count');
    if (countEl) countEl.textContent = `(${txTotal} total)`;

    if (txns.length === 0) {
      list.innerHTML = `<div class="empty-state"><div class="empty-state-icon">📭</div><h3>No transactions found</h3><p>Try adjusting your filters or add a new transaction.</p></div>`;
    } else {
      list.innerHTML = txns.map(t => txItemFull(t)).join('');
    }

    renderPagination(data.totalPages, data.number);
  } catch (err) {
    list.innerHTML = `<div class="empty-state"><p>Failed to load: ${err.message}</p></div>`;
  }
}

function txItemFull(t) {
  const isIncome = t.type === 'INCOME';
  const icon = t.categoryIcon || (isIncome ? 'briefcase' : 'tag');
  const color = t.categoryColor || (isIncome ? 'var(--income-color)' : 'var(--expense-color)');
  return `
    <div class="transaction-item" onclick="openEditTransaction(${JSON.stringify(t).replace(/"/g, '&quot;')})">
      <div class="tx-icon" style="background:${color}22;color:${color}">
        <i class="fas fa-${icon}"></i>
      </div>
      <div class="tx-info">
        <div class="tx-name">${escHtml(t.description || t.categoryName || 'Transaction')}</div>
        <div class="tx-category">
          ${t.categoryName ? `<span style="background:${color}22;color:${color};padding:1px 8px;border-radius:20px;font-size:11px">${t.categoryName}</span>` : ''}
          ${t.walletName ? `<span style="color:var(--text-muted);font-size:11px">• ${t.walletName}</span>` : ''}
          <span style="color:var(--text-muted);font-size:11px">• ${formatDate(t.transactionDate)}</span>
          ${t.recurring ? '<span style="color:var(--accent);font-size:10px">↻ Recurring</span>' : ''}
          ${t.receiptUrl ? '<i class="fas fa-receipt" style="color:var(--warning-color);font-size:11px" title="Has receipt"></i>' : ''}
        </div>
      </div>
      <div style="text-align:right">
        <div class="tx-amount ${isIncome ? 'income' : 'expense'}">${isIncome ? '+' : '-'}${formatCurrency(t.amount)}</div>
      </div>
      <div class="tx-actions">
        <button class="tx-action-btn edit" onclick="event.stopPropagation();openEditTransaction(${JSON.stringify(t).replace(/"/g, '&quot;')})" title="Edit"><i class="fas fa-edit"></i></button>
        <button class="tx-action-btn del" onclick="event.stopPropagation();deleteTransaction(${t.id})" title="Delete"><i class="fas fa-trash"></i></button>
      </div>
    </div>
  `;
}

function renderPagination(totalPages, currentPage) {
  const pag = document.getElementById('tx-pagination');
  if (!pag || totalPages <= 1) { if(pag) pag.innerHTML=''; return; }
  let html = `<button class="page-btn" onclick="changeTxPage(${currentPage-1})" ${currentPage===0?'disabled':''}>
    <i class="fas fa-chevron-left"></i></button>`;
  for (let i = 0; i < totalPages; i++) {
    if (i===0 || i===totalPages-1 || (i>=currentPage-1 && i<=currentPage+1)) {
      html += `<button class="page-btn ${i===currentPage?'active':''}" onclick="changeTxPage(${i})">${i+1}</button>`;
    } else if (i===currentPage-2 || i===currentPage+2) {
      html += `<span style="color:var(--text-muted);padding:0 4px">...</span>`;
    }
  }
  html += `<button class="page-btn" onclick="changeTxPage(${currentPage+1})" ${currentPage===totalPages-1?'disabled':''}>
    <i class="fas fa-chevron-right"></i></button>`;
  pag.innerHTML = html;
}

function changeTxPage(page) {
  txPage = page;
  fetchTransactions();
  document.getElementById('transactions-page').scrollTop = 0;
}

// ==================== TRANSACTION MODAL ====================
function openTransactionModal(tx = null) {
  editingTxId = tx ? tx.id : null;
  const catOptions = allCategories.map(c =>
    `<option value="${c.id}" ${tx?.categoryId===c.id?'selected':''}>${c.name} (${c.type})</option>`
  ).join('');
  const walletOptions = allWallets.map(w =>
    `<option value="${w.id}" ${tx?.walletId===w.id?'selected':''}>${w.name}</option>`
  ).join('');

  const isIncome = tx?.type === 'INCOME';
  openModal(tx ? 'Edit Transaction' : 'Add Transaction', `
    <div class="type-toggle">
      <button type="button" class="type-btn expense ${!isIncome?'active':''}" id="btn-expense" onclick="setTxType('EXPENSE')">
        <i class="fas fa-arrow-up"></i> Expense
      </button>
      <button type="button" class="type-btn income ${isIncome?'active':''}" id="btn-income" onclick="setTxType('INCOME')">
        <i class="fas fa-arrow-down"></i> Income
      </button>
    </div>
    <input type="hidden" id="tx-type-val" value="${tx?.type||'EXPENSE'}"/>
    <div class="form-group">
      <label>Amount *</label>
      <input type="number" id="tx-amount" step="0.01" min="0.01" placeholder="0.00" value="${tx?.amount||''}" required/>
    </div>
    <div class="form-group">
      <label>Description</label>
      <input type="text" id="tx-desc" placeholder="What was this for?" value="${escHtml(tx?.description||'')}"/>
    </div>
    <div class="form-group">
      <label>Date *</label>
      <input type="date" id="tx-date" value="${tx?.transactionDate||todayISO()}" required/>
    </div>
    <div class="form-group">
      <label>Category</label>
      <select id="tx-cat"><option value="">-- Select Category --</option>${catOptions}</select>
    </div>
    <div class="form-group">
      <label>Wallet</label>
      <select id="tx-wallet"><option value="">-- Select Wallet --</option>${walletOptions}</select>
    </div>
    <div class="form-group">
      <label>Notes</label>
      <textarea id="tx-notes" rows="2" placeholder="Optional notes...">${escHtml(tx?.notes||'')}</textarea>
    </div>
    <div class="form-group">
      <label>Receipt Image</label>
      <input type="file" id="tx-receipt" accept="image/*" style="background:var(--bg-tertiary);border:1.5px solid var(--border);border-radius:8px;padding:10px;width:100%;color:var(--text-primary)"/>
      ${tx?.receiptUrl ? `<img src="${tx.receiptUrl}" class="receipt-preview" onclick="window.open('${tx.receiptUrl}','_blank')"/>` : ''}
    </div>
    <div class="form-group" style="display:flex;align-items:center;gap:10px">
      <input type="checkbox" id="tx-recurring" ${tx?.recurring?'checked':''} style="width:16px;height:16px;accent-color:var(--accent)"/>
      <label style="margin:0;cursor:pointer" for="tx-recurring">Mark as recurring</label>
    </div>
    <div class="modal-actions">
      <button class="btn-secondary" onclick="closeModalBtn()">Cancel</button>
      <button class="btn-primary" onclick="saveTransaction()">
        <i class="fas fa-check"></i> ${tx ? 'Update' : 'Save'}
      </button>
    </div>
  `);
}

function openEditTransaction(tx) {
  if (!allCategories.length) {
    API.get('/categories').then(r => { allCategories = r.data || []; openTransactionModal(tx); });
    API.get('/wallets').then(r => { allWallets = r.data || []; });
  } else {
    openTransactionModal(tx);
  }
}

function setTxType(type) {
  document.getElementById('tx-type-val').value = type;
  document.getElementById('btn-expense').classList.toggle('active', type === 'EXPENSE');
  document.getElementById('btn-income').classList.toggle('active', type === 'INCOME');
  document.getElementById('btn-expense').classList.toggle('expense', true);
  document.getElementById('btn-income').classList.toggle('income', true);
}

async function saveTransaction() {
  const amount = parseFloat(document.getElementById('tx-amount').value);
  const date = document.getElementById('tx-date').value;
  if (!amount || amount <= 0) { showToast('Please enter a valid amount', 'error'); return; }
  if (!date) { showToast('Please select a date', 'error'); return; }

  const body = {
    type: document.getElementById('tx-type-val').value,
    amount,
    description: document.getElementById('tx-desc').value.trim() || null,
    transactionDate: date,
    categoryId: document.getElementById('tx-cat').value || null,
    walletId: document.getElementById('tx-wallet').value || null,
    notes: document.getElementById('tx-notes').value.trim() || null,
    recurring: document.getElementById('tx-recurring').checked,
  };

  try {
    showLoading();
    let txData;
    if (editingTxId) {
      txData = await API.put(`/transactions/${editingTxId}`, body);
    } else {
      txData = await API.post('/transactions', body);
    }

    // Upload receipt if selected
    const receiptFile = document.getElementById('tx-receipt').files[0];
    if (receiptFile && txData.data?.id) {
      const fd = new FormData();
      fd.append('file', receiptFile);
      fd.append('transactionId', txData.data.id);
      await API.upload('/files/upload', fd);
    }

    closeModalBtn();
    showToast(`Transaction ${editingTxId ? 'updated' : 'added'} successfully! ✅`, 'success');
    fetchTransactions();
  } catch (err) {
    showToast(err.message, 'error');
  } finally {
    hideLoading();
  }
}

async function deleteTransaction(id) {
  if (!confirm('Delete this transaction? This action cannot be undone.')) return;
  try {
    showLoading();
    await API.delete(`/transactions/${id}`);
    showToast('Transaction deleted', 'info');
    fetchTransactions();
  } catch (err) {
    showToast(err.message, 'error');
  } finally {
    hideLoading();
  }
}

// ==================== VOICE ENTRY ====================
function startVoiceEntry() {
  if (!('webkitSpeechRecognition' in window) && !('SpeechRecognition' in window)) {
    showToast('Voice input not supported in this browser. Try Chrome or Edge.', 'warning');
    return;
  }
  if (isVoiceListening) { stopVoice(); return; }

  const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
  const recognition = new SpeechRecognition();
  recognition.continuous = false;
  recognition.interimResults = false;
  recognition.lang = 'en-IN';

  isVoiceListening = true;
  const btn = document.getElementById('voice-btn');
  if (btn) { btn.classList.add('recording'); btn.innerHTML = '<i class="fas fa-stop"></i> Stop'; }
  document.getElementById('voice-indicator').classList.remove('hidden');

  recognition.start();

  recognition.onresult = (event) => {
    const transcript = event.results[0][0].transcript;
    parseVoiceTransaction(transcript);
    stopVoice();
  };
  recognition.onerror = () => { stopVoice(); showToast('Voice recognition error. Try again.', 'error'); };
  recognition.onend = () => stopVoice();

  window._currentRecognition = recognition;
}

function stopVoice() {
  isVoiceListening = false;
  if (window._currentRecognition) { try { window._currentRecognition.stop(); } catch(e){} }
  const btn = document.getElementById('voice-btn');
  if (btn) { btn.classList.remove('recording'); btn.innerHTML = '<i class="fas fa-microphone"></i> Voice Entry'; }
  document.getElementById('voice-indicator').classList.add('hidden');
}

function parseVoiceTransaction(text) {
  showToast(`🎤 Heard: "${text}"`, 'info');
  const lower = text.toLowerCase();

  // Extract amount — look for numbers
  const amtMatch = lower.match(/(\d+(?:\.\d+)?)\s*(?:rupees?|rs\.?|₹)?/);
  const amount = amtMatch ? parseFloat(amtMatch[1]) : null;

  // Determine type
  const incomeWords = ['received', 'earned', 'got', 'income', 'salary', 'paid me'];
  const expenseWords = ['spent', 'paid', 'bought', 'purchased', 'expense'];
  let type = 'EXPENSE';
  if (incomeWords.some(w => lower.includes(w))) type = 'INCOME';

  // Extract category by matching category names
  let matchedCat = null;
  for (const cat of allCategories) {
    if (lower.includes(cat.name.toLowerCase())) { matchedCat = cat; break; }
  }

  // Common keyword -> category mapping
  if (!matchedCat) {
    const catMap = {
      'food|eat|lunch|dinner|breakfast|restaurant|swiggy|zomato': 'Food & Dining',
      'uber|ola|bus|train|taxi|auto|petrol|fuel': 'Transportation',
      'grocery|supermarket|big bazaar|dmart': 'Food & Dining',
      'amazon|flipkart|shopping|clothes|shirt': 'Shopping',
      'movie|netflix|spotify|game|entertainment': 'Entertainment',
      'doctor|medicine|hospital|pharmacy': 'Healthcare',
    };
    for (const [keywords, catName] of Object.entries(catMap)) {
      if (new RegExp(keywords).test(lower)) {
        matchedCat = allCategories.find(c => c.name === catName);
        break;
      }
    }
  }

  if (!amount) {
    showToast('Could not detect amount. Please add it manually.', 'warning');
  }

  // Pre-fill modal
  openTransactionModal();
  setTimeout(() => {
    if (amount) document.getElementById('tx-amount').value = amount;
    setTxType(type);
    document.getElementById('tx-type-val').value = type;
    document.getElementById('tx-date').value = todayISO();
    document.getElementById('tx-desc').value = text;
    if (matchedCat) document.getElementById('tx-cat').value = matchedCat.id;
    showToast('Voice data applied! Review and save.', 'success');
  }, 300);
}

function escHtml(str) {
  if (!str) return '';
  return str.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
}
