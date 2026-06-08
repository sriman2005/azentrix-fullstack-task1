/* =============================================
   Savings Goals JS — Circular progress rings
   ============================================= */

async function loadSavings() {
  const page = document.getElementById('savings-page');
  page.innerHTML = `<div class="page-header"><h2>Savings Goals</h2></div><div class="skeleton" style="height:400px;border-radius:12px"></div>`;
  try {
    const res = await API.get('/savings-goals');
    renderSavings(res.data || []);
  } catch(err) { page.innerHTML = `<div class="empty-state"><p>Failed to load: ${err.message}</p></div>`; }
}

function renderSavings(goals) {
  const page = document.getElementById('savings-page');
  const totalSaved = goals.reduce((s, g) => s + parseFloat(g.currentAmount||0), 0);
  const totalTarget = goals.reduce((s, g) => s + parseFloat(g.targetAmount||0), 0);
  const completed = goals.filter(g => g.status === 'COMPLETED').length;

  page.innerHTML = `
    <div class="page-header">
      <h2>Savings Goals 🎯</h2>
      <button class="btn-primary" onclick="openGoalModal()"><i class="fas fa-plus"></i> New Goal</button>
    </div>

    ${goals.length > 0 ? `
    <div class="summary-grid" style="grid-template-columns:repeat(3,1fr);margin-bottom:24px">
      <div class="summary-card savings">
        <div class="summary-card-icon"><i class="fas fa-coins"></i></div>
        <div class="summary-card-label">Total Saved</div>
        <div class="summary-card-value">${formatCurrency(totalSaved)}</div>
      </div>
      <div class="summary-card balance">
        <div class="summary-card-icon"><i class="fas fa-bullseye"></i></div>
        <div class="summary-card-label">Total Target</div>
        <div class="summary-card-value">${formatCurrency(totalTarget)}</div>
      </div>
      <div class="summary-card income">
        <div class="summary-card-icon"><i class="fas fa-check-circle"></i></div>
        <div class="summary-card-label">Goals Completed</div>
        <div class="summary-card-value">${completed} / ${goals.length}</div>
      </div>
    </div>` : ''}

    ${goals.length === 0 ? `
      <div class="empty-state">
        <div class="empty-state-icon">🐷</div>
        <h3>No savings goals yet</h3>
        <p>Set goals for your dreams — vacation, emergency fund, gadgets!</p>
        <button class="btn-primary" style="margin-top:16px" onclick="openGoalModal()"><i class="fas fa-plus"></i> Create Goal</button>
      </div>
    ` : `
      <div style="display:grid;grid-template-columns:repeat(auto-fill,minmax(280px,1fr));gap:16px">
        ${goals.map(g => goalCard(g)).join('')}
      </div>
    `}
  `;
}

function goalCard(g) {
  const pct = Math.min(g.progressPercentage || 0, 100);
  const r = 50; const circ = 2 * Math.PI * r;
  const offset = circ - (pct / 100) * circ;
  const color = g.color || '#10b981';
  const isCompleted = g.status === 'COMPLETED';

  return `
    <div class="goal-card ${isCompleted ? 'completed' : ''}">
      <div class="goal-header">
        <div style="display:flex;align-items:center;gap:12px">
          <div class="goal-icon" style="background:${color}22;color:${color}">
            <i class="fas fa-${g.icon||'piggy-bank'}"></i>
          </div>
          <div>
            <div class="goal-name">${escHtml(g.name)}</div>
            ${g.deadline ? `<div class="goal-deadline">🗓️ ${formatDate(g.deadline)} · ${g.daysRemaining} days left</div>` : ''}
          </div>
        </div>
        <span class="badge ${g.status==='COMPLETED'?'completed':g.status==='PAUSED'?'inactive':'active'}">${g.status}</span>
      </div>

      <div style="display:flex;align-items:center;gap:20px">
        <div class="progress-ring">
          <svg width="120" height="120" viewBox="0 0 120 120">
            <circle cx="60" cy="60" r="${r}" fill="none" stroke="var(--bg-tertiary)" stroke-width="8"/>
            <circle cx="60" cy="60" r="${r}" fill="none" stroke="${color}" stroke-width="8"
              stroke-dasharray="${circ}" stroke-dashoffset="${offset}" stroke-linecap="round"
              style="transition:stroke-dashoffset 1s ease"/>
          </svg>
          <div class="progress-ring-text">
            <span style="font-size:16px;font-weight:800;color:${color}">${pct.toFixed(0)}%</span>
            <span style="font-size:10px;color:var(--text-muted)">done</span>
          </div>
        </div>
        <div style="flex:1">
          <div style="font-size:12px;color:var(--text-muted);margin-bottom:4px">Saved</div>
          <div style="font-size:18px;font-weight:800;color:var(--text-primary)">${formatCurrency(g.currentAmount)}</div>
          <div style="font-size:12px;color:var(--text-muted);margin-top:4px">Target: ${formatCurrency(g.targetAmount)}</div>
          <div style="font-size:12px;color:var(--income-color);margin-top:2px">${formatCurrency(g.remainingAmount)} to go</div>
        </div>
      </div>

      <div style="display:flex;gap:8px;margin-top:16px">
        ${!isCompleted ? `<button class="btn-primary" style="flex:1;padding:8px;font-size:13px" onclick="openContributeModal(${g.id},'${escHtml(g.name)}','${color}')">
          <i class="fas fa-plus"></i> Contribute
        </button>` : '<div class="badge completed" style="flex:1;justify-content:center;padding:8px"><i class="fas fa-trophy"></i> Goal Achieved! 🎉</div>'}
        <button class="tx-action-btn edit" onclick="openGoalModal(${JSON.stringify(g).replace(/"/g,'&quot;')})"><i class="fas fa-edit"></i></button>
        <button class="tx-action-btn del" onclick="deleteGoal(${g.id})"><i class="fas fa-trash"></i></button>
      </div>
    </div>
  `;
}

function openGoalModal(g = null) {
  const COLORS = ['#10b981','#6366f1','#f59e0b','#ef4444','#06b6d4','#8b5cf6','#ec4899','#f97316'];
  const ICONS = ['piggy-bank','home','plane','car','graduation-cap','heart','star','gift','laptop','gamepad'];
  openModal(g ? 'Edit Goal' : 'New Savings Goal', `
    <div class="form-group">
      <label>Goal Name *</label>
      <input type="text" id="goal-name" placeholder="e.g. Emergency Fund" value="${escHtml(g?.name||'')}"/>
    </div>
    <div class="form-group">
      <label>Target Amount (${App.currency}) *</label>
      <input type="number" id="goal-target" step="0.01" min="1" placeholder="e.g. 50000" value="${g?.targetAmount||''}"/>
    </div>
    ${!g ? `<div class="form-group">
      <label>Initial Amount (${App.currency})</label>
      <input type="number" id="goal-initial" step="0.01" min="0" placeholder="0" value="0"/>
    </div>` : ''}
    <div class="form-group">
      <label>Deadline</label>
      <input type="date" id="goal-deadline" value="${g?.deadline||''}"/>
    </div>
    <div class="form-group">
      <label>Color</label>
      <div class="color-picker-row" id="goal-color-row">
        ${COLORS.map(c => `<div class="color-swatch ${(g?.color||'#10b981')===c?'selected':''}" style="background:${c}" onclick="selectGoalColor('${c}')" data-color="${c}"></div>`).join('')}
      </div>
      <input type="hidden" id="goal-color" value="${g?.color||'#10b981'}"/>
    </div>
    <div class="form-group">
      <label>Icon</label>
      <div style="display:flex;gap:8px;flex-wrap:wrap;margin-top:6px">
        ${ICONS.map(ic => `<button type="button" onclick="selectGoalIcon('${ic}')" id="icon-${ic}" style="width:36px;height:36px;border-radius:8px;background:${(g?.icon||'piggy-bank')===ic?'var(--gradient-brand)':'var(--bg-tertiary)'};color:${(g?.icon||'piggy-bank')===ic?'white':'var(--text-secondary)'};border:1px solid var(--border);transition:all 0.2s">
          <i class="fas fa-${ic}"></i>
        </button>`).join('')}
      </div>
      <input type="hidden" id="goal-icon" value="${g?.icon||'piggy-bank'}"/>
    </div>
    <div class="modal-actions">
      <button class="btn-secondary" onclick="closeModalBtn()">Cancel</button>
      <button class="btn-primary" onclick="saveGoal(${g?.id||'null'})">
        <i class="fas fa-check"></i> ${g ? 'Update' : 'Create'}
      </button>
    </div>
  `);
}

function selectGoalColor(color) {
  document.querySelectorAll('.color-swatch').forEach(s => s.classList.toggle('selected', s.dataset.color === color));
  document.getElementById('goal-color').value = color;
}

function selectGoalIcon(icon) {
  document.getElementById('goal-icon').value = icon;
  document.querySelectorAll('[id^="icon-"]').forEach(btn => {
    const isSelected = btn.id === `icon-${icon}`;
    btn.style.background = isSelected ? 'var(--gradient-brand)' : 'var(--bg-tertiary)';
    btn.style.color = isSelected ? 'white' : 'var(--text-secondary)';
  });
}

async function saveGoal(existingId) {
  const name = document.getElementById('goal-name').value.trim();
  const target = parseFloat(document.getElementById('goal-target').value);
  const initial = document.getElementById('goal-initial') ? parseFloat(document.getElementById('goal-initial').value||0) : undefined;
  if (!name) { showToast('Please enter a goal name', 'error'); return; }
  if (!target || target <= 0) { showToast('Please enter a valid target amount', 'error'); return; }

  try {
    showLoading();
    const body = {
      name, targetAmount: target,
      deadline: document.getElementById('goal-deadline').value || null,
      icon: document.getElementById('goal-icon').value,
      color: document.getElementById('goal-color').value,
    };
    if (initial !== undefined) body.initialAmount = initial;
    if (existingId) await API.put(`/savings-goals/${existingId}`, body);
    else await API.post('/savings-goals', body);
    closeModalBtn();
    showToast(`Goal ${existingId ? 'updated' : 'created'}! 🐷`, 'success');
    loadSavings();
  } catch(err) { showToast(err.message, 'error'); } finally { hideLoading(); }
}

function openContributeModal(goalId, goalName, color) {
  openModal(`Contribute to "${goalName}"`, `
    <div style="text-align:center;margin-bottom:20px">
      <i class="fas fa-piggy-bank" style="font-size:40px;color:${color}"></i>
    </div>
    <div class="form-group">
      <label>Amount to Add (${App.currency}) *</label>
      <input type="number" id="contrib-amount" step="0.01" min="0.01" placeholder="e.g. 1000"/>
    </div>
    <div style="display:flex;gap:8px;margin-bottom:16px;flex-wrap:wrap">
      ${[500,1000,2000,5000].map(a => `<button class="btn-secondary" style="padding:6px 14px;font-size:13px" onclick="document.getElementById('contrib-amount').value=${a}">+${formatCurrency(a)}</button>`).join('')}
    </div>
    <div class="modal-actions">
      <button class="btn-secondary" onclick="closeModalBtn()">Cancel</button>
      <button class="btn-primary" onclick="contribute(${goalId})"><i class="fas fa-check"></i> Add</button>
    </div>
  `);
}

async function contribute(goalId) {
  const amount = parseFloat(document.getElementById('contrib-amount').value);
  if (!amount || amount <= 0) { showToast('Please enter a valid amount', 'error'); return; }
  try {
    showLoading();
    await API.post(`/savings-goals/${goalId}/contribute`, { amount });
    closeModalBtn();
    showToast('Contribution added! 🎉', 'success');
    loadSavings();
  } catch(err) { showToast(err.message, 'error'); } finally { hideLoading(); }
}

async function deleteGoal(id) {
  if (!confirm('Delete this savings goal?')) return;
  try {
    showLoading();
    await API.delete(`/savings-goals/${id}`);
    showToast('Goal deleted', 'info');
    loadSavings();
  } catch(err) { showToast(err.message, 'error'); } finally { hideLoading(); }
}
