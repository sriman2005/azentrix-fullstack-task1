/* Calendar JS */
let calYear, calMonth, calTxMap = {};

async function loadCalendar() {
  const now = new Date();
  calYear = now.getFullYear();
  calMonth = now.getMonth();
  renderCalendar();
}

async function renderCalendar() {
  const page = document.getElementById('calendar-page');
  const monthName = new Date(calYear, calMonth).toLocaleDateString('en-IN', {month:'long',year:'numeric'});
  page.innerHTML = `
    <div class="page-header">
      <h2>Calendar 📅</h2>
      <div style="display:flex;align-items:center;gap:12px">
        <button class="icon-btn" onclick="changeCalMonth(-1)"><i class="fas fa-chevron-left"></i></button>
        <span style="font-size:16px;font-weight:600;min-width:160px;text-align:center">${monthName}</span>
        <button class="icon-btn" onclick="changeCalMonth(1)"><i class="fas fa-chevron-right"></i></button>
        <button class="btn-secondary" style="padding:7px 16px;font-size:13px" onclick="goToday()">Today</button>
      </div>
    </div>
    <div class="card">
      <div class="calendar-grid" id="cal-header">
        ${['Sun','Mon','Tue','Wed','Thu','Fri','Sat'].map(d=>`<div class="calendar-header-day">${d}</div>`).join('')}
      </div>
      <div style="margin-top:4px">
        <div class="skeleton" id="cal-loading" style="height:400px;border-radius:12px"></div>
        <div class="calendar-grid" id="cal-body" style="display:none;gap:4px"></div>
      </div>
    </div>
    <div class="card" id="cal-day-detail" style="margin-top:16px;display:none">
      <div class="card-title" id="cal-detail-title">Day Transactions</div>
      <div id="cal-detail-list" class="transaction-list"></div>
    </div>
  `;

  try {
    const res = await API.get(`/transactions/month?year=${calYear}&month=${calMonth+1}`);
    const txns = res.data || [];
    calTxMap = {};
    txns.forEach(t => {
      const key = t.transactionDate;
      if (!calTxMap[key]) calTxMap[key] = [];
      calTxMap[key].push(t);
    });
    buildCalendarGrid();
  } catch(err) {
    document.getElementById('cal-loading').style.height = '60px';
    document.getElementById('cal-loading').innerHTML = `<p style="padding:16px;color:var(--expense-color)">Failed: ${err.message}</p>`;
  }
}

function buildCalendarGrid() {
  const body = document.getElementById('cal-body');
  const loading = document.getElementById('cal-loading');
  if (!body) return;
  loading.style.display = 'none';
  body.style.display = 'grid';

  const firstDay = new Date(calYear, calMonth, 1).getDay();
  const daysInMonth = new Date(calYear, calMonth+1, 0).getDate();
  const daysInPrev = new Date(calYear, calMonth, 0).getDate();
  const today = new Date();
  let html = '';

  // Previous month filler
  for (let i = firstDay - 1; i >= 0; i--) {
    html += `<div class="calendar-day other-month"><div class="day-number">${daysInPrev - i}</div></div>`;
  }

  // Current month
  for (let d = 1; d <= daysInMonth; d++) {
    const dateStr = `${calYear}-${String(calMonth+1).padStart(2,'0')}-${String(d).padStart(2,'0')}`;
    const txns = calTxMap[dateStr] || [];
    const isToday = today.getFullYear()===calYear && today.getMonth()===calMonth && today.getDate()===d;
    const hasIncome = txns.some(t => t.type==='INCOME');
    const hasExpense = txns.some(t => t.type==='EXPENSE');
    html += `
      <div class="calendar-day ${isToday?'today':''}" onclick="showCalDay('${dateStr}')">
        <div class="day-number">${d}</div>
        ${txns.length > 0 ? `<div class="day-dots">
          ${hasIncome?'<div class="day-dot income"></div>':''}
          ${hasExpense?'<div class="day-dot expense"></div>':''}
          ${txns.length > 2 ? `<span style="font-size:9px;color:var(--text-muted)">+${txns.length-2}</span>` : ''}
        </div>` : ''}
      </div>
    `;
  }

  // Next month filler
  const totalCells = Math.ceil((firstDay + daysInMonth) / 7) * 7;
  for (let d = 1; d <= totalCells - firstDay - daysInMonth; d++) {
    html += `<div class="calendar-day other-month"><div class="day-number">${d}</div></div>`;
  }

  body.innerHTML = html;
}

function showCalDay(dateStr) {
  document.querySelectorAll('.calendar-day.selected').forEach(d => d.classList.remove('selected'));
  const txns = calTxMap[dateStr] || [];
  const detail = document.getElementById('cal-day-detail');
  const title = document.getElementById('cal-detail-title');
  const list = document.getElementById('cal-detail-list');
  detail.style.display = 'block';
  title.textContent = `${formatDate(dateStr)} — ${txns.length} transaction${txns.length!==1?'s':''}`;
  if (!txns.length) {
    list.innerHTML = `<div class="empty-state" style="padding:20px"><p>No transactions on this day. <a href="#" onclick="openTransactionModal();return false">Add one?</a></p></div>`;
  } else {
    const income = txns.filter(t=>t.type==='INCOME').reduce((s,t)=>s+parseFloat(t.amount),0);
    const expense = txns.filter(t=>t.type==='EXPENSE').reduce((s,t)=>s+parseFloat(t.amount),0);
    list.innerHTML = `
      <div style="display:flex;gap:16px;margin-bottom:14px;font-size:13px">
        <span style="color:var(--income-color)"><i class="fas fa-arrow-down"></i> ${formatCurrency(income)}</span>
        <span style="color:var(--expense-color)"><i class="fas fa-arrow-up"></i> ${formatCurrency(expense)}</span>
      </div>
      ${txns.map(t => txItemFull ? txItemFull(t) : txItem(t)).join('')}
    `;
  }
  detail.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
}

function changeCalMonth(dir) {
  calMonth += dir;
  if (calMonth < 0) { calMonth = 11; calYear--; }
  if (calMonth > 11) { calMonth = 0; calYear++; }
  renderCalendar();
}

function goToday() {
  const now = new Date();
  calYear = now.getFullYear();
  calMonth = now.getMonth();
  renderCalendar();
}
