/* =============================================
   Dashboard JS — Charts, Summary, Insights
   ============================================= */

let dashPieChart = null, dashBarChart = null, dashLineChart = null;

async function loadDashboard() {
  const page = document.getElementById('dashboard-page');
  page.innerHTML = buildDashboardSkeleton();

  try {
    const [dashRes, insightRes, healthRes, catRes, monthlyRes, recentRes] = await Promise.all([
      API.get('/analytics/dashboard'),
      API.get('/analytics/insights'),
      API.get('/analytics/health-score'),
      API.get(`/analytics/category-breakdown?year=${new Date().getFullYear()}&month=${new Date().getMonth()+1}`),
      API.get(`/analytics/monthly-comparison?year=${new Date().getFullYear()}`),
      API.get('/transactions?size=6'),
    ]);

    const dash = dashRes.data;
    const insights = insightRes.data;
    const health = healthRes.data;
    const cats = catRes.data;
    const monthly = monthlyRes.data;
    const recent = recentRes.data;

    page.innerHTML = buildDashboardHTML(dash, health, recent);
    renderDashCharts(cats, monthly);
    renderInsights(insights);
    renderHealthGauge(health);

  } catch (err) {
    page.innerHTML = `<div class="empty-state"><div class="empty-state-icon">📊</div><h3>Failed to load dashboard</h3><p>${err.message}</p><button class="btn-primary" onclick="loadDashboard()">Retry</button></div>`;
  }
}

function buildDashboardHTML(dash, health, recent) {
  const monthNames = ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec'];
  const month = monthNames[(dash.month || 1) - 1];
  const year = dash.year;
  const incomeChange = dash.incomeChange || 0;
  const expenseChange = dash.expenseChange || 0;

  return `
    <div class="page-header">
      <h2>Dashboard 📊</h2>
      <div class="topbar-currency" style="font-size:13px;color:var(--text-muted)">${month} ${year}</div>
    </div>

    <!-- Summary Cards -->
    <div class="summary-grid">
      <div class="summary-card income">
        <div class="summary-card-icon"><i class="fas fa-arrow-down"></i></div>
        <div class="summary-card-label">Total Income</div>
        <div class="summary-card-value" id="dash-income">${formatCurrency(dash.totalIncome)}</div>
        <div class="summary-card-change ${incomeChange >= 0 ? 'positive' : 'negative'}">
          <i class="fas fa-${incomeChange >= 0 ? 'arrow-up' : 'arrow-down'}"></i>
          ${Math.abs(incomeChange).toFixed(1)}% vs last month
        </div>
      </div>
      <div class="summary-card expense">
        <div class="summary-card-icon"><i class="fas fa-arrow-up"></i></div>
        <div class="summary-card-label">Total Expense</div>
        <div class="summary-card-value" id="dash-expense">${formatCurrency(dash.totalExpense)}</div>
        <div class="summary-card-change ${expenseChange <= 0 ? 'positive' : 'negative'}">
          <i class="fas fa-${expenseChange <= 0 ? 'arrow-down' : 'arrow-up'}"></i>
          ${Math.abs(expenseChange).toFixed(1)}% vs last month
        </div>
      </div>
      <div class="summary-card balance">
        <div class="summary-card-icon"><i class="fas fa-balance-scale"></i></div>
        <div class="summary-card-label">Net Balance</div>
        <div class="summary-card-value" id="dash-balance" style="color:${parseFloat(dash.balance)>=0?'var(--income-color)':'var(--expense-color)'}">${formatCurrency(dash.balance)}</div>
        <div class="summary-card-change positive"><i class="fas fa-chart-line"></i> This month</div>
      </div>
      <div class="summary-card savings">
        <div class="summary-card-icon"><i class="fas fa-piggy-bank"></i></div>
        <div class="summary-card-label">Savings Rate</div>
        <div class="summary-card-value" id="dash-savings">${dash.savingsRate || 0}%</div>
        <div class="summary-card-change ${parseFloat(dash.savingsRate||0)>=20?'positive':'negative'}">
          <i class="fas fa-${parseFloat(dash.savingsRate||0)>=20?'check':'exclamation'}"></i>
          ${parseFloat(dash.savingsRate||0)>=20?'Healthy rate':'Aim for 20%+'}
        </div>
      </div>
    </div>

    <!-- Charts Row -->
    <div class="charts-grid">
      <div class="card">
        <div class="card-title"><span><i class="fas fa-chart-pie" style="color:var(--accent)"></i> Expenses by Category</span></div>
        <div class="chart-container"><canvas id="dash-pie-chart"></canvas></div>
      </div>
      <div class="card">
        <div class="card-title"><span><i class="fas fa-chart-bar" style="color:var(--accent)"></i> Monthly Overview</span></div>
        <div class="chart-container"><canvas id="dash-bar-chart"></canvas></div>
      </div>
    </div>

    <!-- Bottom Row: Health Score + Insights + Recent -->
    <div class="charts-grid" style="grid-template-columns:1fr 1fr">
      <div class="card">
        <div class="card-title"><span><i class="fas fa-heart-pulse" style="color:#ef4444"></i> Financial Health Score</span></div>
        <div id="health-score-widget"></div>
      </div>
      <div class="card">
        <div class="card-title"><span><i class="fas fa-lightbulb" style="color:var(--warning-color)"></i> Smart Insights</span>
          <button class="btn-secondary" style="padding:4px 12px;font-size:12px" onclick="loadDashboard()"><i class="fas fa-sync-alt"></i></button>
        </div>
        <div id="insights-container" class="insights-list"></div>
      </div>
    </div>

    <!-- Recent Transactions -->
    <div class="card" style="margin-top:16px">
      <div class="card-title">
        <span><i class="fas fa-clock" style="color:var(--accent)"></i> Recent Transactions</span>
        <button class="btn-secondary" style="padding:6px 14px;font-size:13px" onclick="navigateTo('transactions')">View All</button>
      </div>
      <div id="recent-txns" class="transaction-list">
        ${recent.content && recent.content.length ? recent.content.map(t => txItem(t)).join('') :
          '<div class="empty-state"><div class="empty-state-icon">📋</div><h3>No transactions yet</h3><p>Add your first transaction!</p></div>'}
      </div>
    </div>
  `;
}

function buildDashboardSkeleton() {
  return `
    <div class="summary-grid">
      ${[...Array(4)].map(() => `<div class="summary-card"><div class="skeleton" style="height:120px"></div></div>`).join('')}
    </div>
    <div class="charts-grid">
      <div class="card"><div class="skeleton" style="height:280px"></div></div>
      <div class="card"><div class="skeleton" style="height:280px"></div></div>
    </div>
  `;
}

function txItem(t) {
  const isIncome = t.type === 'INCOME';
  const icon = t.categoryIcon || (isIncome ? 'briefcase' : 'tag');
  const color = t.categoryColor || (isIncome ? 'var(--income-color)' : 'var(--expense-color)');
  return `
    <div class="transaction-item" onclick="openEditTransaction(${JSON.stringify(t).replace(/"/g, '&quot;')})">
      <div class="tx-icon" style="background:${color}22;color:${color}">
        <i class="fas fa-${icon}"></i>
      </div>
      <div class="tx-info">
        <div class="tx-name">${t.description || t.categoryName || 'Transaction'}</div>
        <div class="tx-category">${t.categoryName || ''} • ${formatDate(t.transactionDate)}</div>
      </div>
      <div class="tx-amount ${isIncome ? 'income' : 'expense'}">${isIncome ? '+' : '-'}${formatCurrency(t.amount)}</div>
    </div>
  `;
}

function renderDashCharts(cats, monthly) {
  const isDark = document.body.classList.contains('dark-mode');
  Chart.defaults.color = isDark ? '#94a3b8' : '#64748b';

  // Pie Chart
  const pieCtx = document.getElementById('dash-pie-chart');
  if (pieCtx) {
    if (dashPieChart) dashPieChart.destroy();
    const catData = cats.categories || [];
    dashPieChart = new Chart(pieCtx, {
      type: 'doughnut',
      data: {
        labels: catData.map(c => c.name),
        datasets: [{
          data: catData.map(c => c.amount),
          backgroundColor: ['#6366f1','#8b5cf6','#ec4899','#ef4444','#f97316','#f59e0b','#10b981','#06b6d4','#3b82f6','#a855f7'],
          borderWidth: 0, hoverOffset: 8
        }]
      },
      options: {
        responsive: true, maintainAspectRatio: false,
        cutout: '65%',
        plugins: {
          legend: { position: 'bottom', labels: { padding: 16, font: { size: 12 }, usePointStyle: true } },
          tooltip: { callbacks: { label: (ctx) => ` ${ctx.label}: ${formatCurrency(ctx.raw)}` } }
        }
      }
    });
  }

  // Bar Chart
  const barCtx = document.getElementById('dash-bar-chart');
  if (barCtx) {
    if (dashBarChart) dashBarChart.destroy();
    dashBarChart = new Chart(barCtx, {
      type: 'bar',
      data: {
        labels: monthly.labels || [],
        datasets: [
          { label: 'Income', data: monthly.income || [], backgroundColor: 'rgba(16,185,129,0.7)', borderRadius: 6, borderSkipped: false },
          { label: 'Expense', data: monthly.expense || [], backgroundColor: 'rgba(239,68,68,0.7)', borderRadius: 6, borderSkipped: false }
        ]
      },
      options: {
        responsive: true, maintainAspectRatio: false,
        scales: {
          x: { grid: { display: false } },
          y: { grid: { color: isDark ? 'rgba(255,255,255,0.05)' : 'rgba(0,0,0,0.05)' }, ticks: { callback: v => App.currency + v.toLocaleString() } }
        },
        plugins: { legend: { position: 'top', labels: { font: { size: 12 }, usePointStyle: true } } }
      }
    });
  }
}

function renderInsights(insights) {
  const container = document.getElementById('insights-container');
  if (!container) return;
  if (!insights || insights.length === 0) {
    container.innerHTML = '<div class="empty-state" style="padding:20px"><p>No insights yet. Keep tracking!</p></div>';
    return;
  }
  container.innerHTML = insights.map(i => `
    <div class="insight-item ${i.type}">
      <div class="insight-icon"><i class="fas fa-${i.icon}"></i></div>
      <div class="insight-content">
        <h4>${i.title}</h4>
        <p>${i.message}</p>
      </div>
    </div>
  `).join('');
}

function renderHealthGauge(health) {
  const widget = document.getElementById('health-score-widget');
  if (!widget) return;
  const score = health.score || 0;
  const grade = health.grade || 'Fair';
  const gradeColors = { Excellent: '#10b981', Good: '#06b6d4', Fair: '#f59e0b', Poor: '#f97316', Critical: '#ef4444' };
  const color = gradeColors[grade] || '#6366f1';
  const r = 56; const circ = 2 * Math.PI * r;
  const offset = circ - (score / 100) * circ;

  widget.innerHTML = `
    <div class="health-score-container">
      <div class="health-score-gauge">
        <svg width="140" height="140" viewBox="0 0 140 140">
          <circle cx="70" cy="70" r="${r}" fill="none" stroke="var(--bg-tertiary)" stroke-width="10" stroke-dasharray="${circ * 0.75}" stroke-linecap="round"/>
          <circle cx="70" cy="70" r="${r}" fill="none" stroke="${color}" stroke-width="10"
            stroke-dasharray="${circ * 0.75 * score/100} ${circ * 0.75 * (1-score/100)}" stroke-linecap="round"
            style="transition:stroke-dasharray 1s ease"/>
        </svg>
        <div class="health-score-value">
          <span class="health-score-number" style="color:${color}">${score}</span>
          <span class="health-score-grade" style="color:${color}">${grade}</span>
        </div>
      </div>
      <div class="health-score-details" style="flex:1">
        <p style="font-size:13px;color:var(--text-secondary);margin-bottom:14px;line-height:1.5">${health.message}</p>
        ${Object.entries(health.breakdown || {}).map(([k, v]) => `
          <div class="health-detail-item">
            <div>
              <span class="health-detail-label">${k.replace(/Score/, ' Score').replace(/([A-Z])/g, ' $1').trim()}</span>
              <div class="health-detail-bar"><div class="health-detail-fill" style="width:${Math.min(v * 4, 100)}%"></div></div>
            </div>
            <strong style="color:var(--text-primary);font-size:14px">${v}/25</strong>
          </div>
        `).join('')}
      </div>
    </div>
  `;
}
