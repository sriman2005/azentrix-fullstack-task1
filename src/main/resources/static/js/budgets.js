/* =============================================
   Budgets JS — Budget limits & progress bars
   ============================================= */

let budgetCategories = [];

async function loadBudgets() {
  const page = document.getElementById('budgets-page');
  page.innerHTML = `<div class="page-header"><h2>Budgets</h2></div><div class="skeleton" style="height:400px;border-radius:12px"></div>`;
  try {
    const now = new Date();
    const [budgetsRes, catsRes] = await Promise.all([
      API.get(`/budgets/month?year=${now.getFullYear()}&month=${now.getMonth()+1}`),
      API.get('/categories')
    ]);
    budgetCategories = (catsRes.data||[]).filter(c => c.type === 'EXPENSE');
    renderBudgets(budgetsRes.data || [], now);
  } catch(err) {
    page.innerHTML = `<div class="empty-state"><p>Failed to load: ${err.message}</p></div>`;
  }
}

function renderBudgets(budgets, now) {
  const page = document.getElementById('budgets-page');
  const monthName = now.toLocaleDateString('en-IN', { month: 'long', year: 'numeric' });
  const exceeded = budgets.filter(b => b.status === 'EXCEEDED').length;
  const warning = budgets.filter(b => b.status === 'WARNING').length;

  page.innerHTML = `
    <div class="page-header">
      <h2>Budgets 🎯</h2>
      <div class="page-header-actions">
        <span style="font-size:13px;color:var(--text-muted)">${monthName}</span>
        <button class="btn-primary" onclick="openBudgetModal()"><i class="fas fa-plus"></i> Add Budget</button>
      </div>
    </div>

    ${exceeded > 0 ? `<div class="insight-item warning" style="margin-bottom:16px">
      <div class="insight-icon"><i class="fas fa-exclamation-triangle"></i></div>
      <div class="insight-content"><h4>${exceeded} budget(s) exceeded!</h4><p>You've gone over budget in ${exceeded} category${exceeded>1?'s':''}. Review your spending.</p></div>
    </div>` : ''}
    ${warning > 0 ? `<div class="insight-item info" style="margin-bottom:16px">
      <div class="insight-icon"><i class="fas fa-bell"></i></div>
      <div class="insight-content"><h4>${warning} budget(s) approaching limit</h4><p>You're close to the limit in ${warning} category${warning>1?'s':''}.</p></div>
    </div>` : ''}

    ${budgets.length === 0 ? `
      <div class="empty-state">
        <div class="empty-state-icon">🎯</div>
        <h3>No budgets set for this month</h3>
        <p>Create budget limits to track your spending and get alerts</p>
        <button class="btn-primary" style="margin-top:16px" onclick="openBudgetModal()"><i class="fas fa-plus"></i> Create Budget</button>
      </div>
    ` : `
      <div style="display:grid;grid-template-columns:repeat(auto-fill,minmax(300px,1fr));gap:16px">
        ${budgets.map(b => budgetCard(b)).join('')}
      </div>
    `}
  `;
}

function budgetCard(b) {
  const pct = Math.min(b.percentageUsed || 0, 100);
  const statusClass = b.status === 'EXCEEDED' ? 'exceeded' : b.status === 'WARNING' ? 'warning' : 'safe';
  const statusIcon = b.status === 'EXCEEDED' ? 'times-circle' : b.status === 'WARNING' ? 'exclamation-circle' : 'check-circle';
  return `
    <div class="card" style="border-left:3px solid ${b.status==='EXCEEDED'?'var(--expense-color)':b.status==='WARNING'?'var(--warning-color)':'var(--income-color)'}">
      <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:12px">
        <div style="display:flex;align-items:center;gap:10px">
          <div style="width:38px;height:38px;border-radius:10px;background:${b.categoryColor}22;color:${b.categoryColor};display:flex;align-items:center;justify-content:center;font-size:16px">
            <i class="fas fa-${b.categoryIcon||'tag'}"></i>
          </div>
          <div>
            <div style="font-size:14px;font-weight:600;color:var(--text-primary)">${b.categoryName}</div>
            <span class="badge ${statusClass}"><i class="fas fa-${statusIcon}"></i> ${b.status}</span>
          </div>
        </div>
        <div style="display:flex;gap:6px">
          <button class="tx-action-btn edit" onclick="openBudgetModal(${JSON.stringify(b).replace(/"/g,'&quot;')})"><i class="fas fa-edit"></i></button>
          <button class="tx-action-btn del" onclick="deleteBudget(${b.id})"><i class="fas fa-trash"></i></button>
        </div>
      </div>

      <div style="display:flex;justify-content:space-between;font-size:13px;margin-bottom:6px">
        <span style="color:var(--text-muted)">Spent</span>
        <span style="font-weight:700;color:var(--text-primary)">${formatCurrency(b.spentAmount)} / ${formatCurrency(b.limitAmount)}</span>
      </div>
      <div class="progress-bar">
        <div class="progress-fill ${statusClass}" style="width:${pct}%"></div>
      </div>
      <div style="display:flex;justify-content:space-between;margin-top:8px;font-size:12px">
        <span style="color:var(--text-muted)">${pct.toFixed(1)}% used</span>
        <span style="color:${b.remainingAmount>=0?'var(--income-color)':'var(--expense-color)'};font-weight:600">
          ${b.remainingAmount >= 0 ? formatCurrency(b.remainingAmount) + ' left' : formatCurrency(Math.abs(b.remainingAmount)) + ' over'}
        </span>
      </div>
      ${b.alertThreshold ? `<div style="font-size:11px;color:var(--text-muted);margin-top:6px">Alert at ${b.alertThreshold}%</div>` : ''}
    </div>
  `;
}

function openBudgetModal(b = null) {
  const now = new Date();
  const catOptions = budgetCategories.map(c =>
    `<option value="${c.id}" ${b?.categoryId===c.id?'selected':''}>${c.name}</option>`
  ).join('');
  openModal(b ? 'Edit Budget' : 'Set Budget', `
    <div class="form-group">
      <label>Category *</label>
      <select id="budget-cat" ${b?'disabled':''}>
        <option value="">-- Select Category --</option>${catOptions}
      </select>
    </div>
    <div class="form-group">
      <label>Monthly Limit (${App.currency}) *</label>
      <input type="number" id="budget-limit" step="0.01" min="1" placeholder="e.g. 5000" value="${b?.limitAmount||''}"/>
    </div>
    <div class="form-group">
      <label>Alert At (%)</label>
      <input type="number" id="budget-alert" min="1" max="100" value="${b?.alertThreshold||80}"/>
    </div>
    <div class="modal-actions">
      <button class="btn-secondary" onclick="closeModalBtn()">Cancel</button>
      <button class="btn-primary" onclick="saveBudget(${b?.id||'null'}, ${b?`'${b.categoryId}'`:'null'})">
        <i class="fas fa-check"></i> ${b ? 'Update' : 'Save'}
      </button>
    </div>
  `);
}

async function saveBudget(existingId, existingCatId) {
  const catId = existingCatId || document.getElementById('budget-cat').value;
  const limit = parseFloat(document.getElementById('budget-limit').value);
  const alert = parseFloat(document.getElementById('budget-alert').value) || 80;
  const now = new Date();

  if (!catId) { showToast('Please select a category', 'error'); return; }
  if (!limit || limit <= 0) { showToast('Please enter a valid limit', 'error'); return; }

  try {
    showLoading();
    const body = { categoryId: parseInt(catId), limitAmount: limit, alertThreshold: alert, period: 'MONTHLY', month: now.getMonth()+1, year: now.getFullYear() };
    if (existingId) await API.put(`/budgets/${existingId}`, body);
    else await API.post('/budgets', body);
    closeModalBtn();
    showToast(`Budget ${existingId ? 'updated' : 'created'}! 🎯`, 'success');
    loadBudgets();
  } catch(err) { showToast(err.message, 'error'); } finally { hideLoading(); }
}

async function deleteBudget(id) {
  if (!confirm('Delete this budget?')) return;
  try {
    showLoading();
    await API.delete(`/budgets/${id}`);
    showToast('Budget deleted', 'info');
    loadBudgets();
  } catch(err) { showToast(err.message, 'error'); } finally { hideLoading(); }
}
