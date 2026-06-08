/* Analytics JS */
let lineChart = null, catBarChart = null;

async function loadAnalytics() {
  const page = document.getElementById('analytics-page');
  const now = new Date();
  page.innerHTML = `<div class="page-header"><h2>Analytics</h2></div><div class="skeleton" style="height:500px;border-radius:12px"></div>`;
  try {
    const year = now.getFullYear();
    const month = now.getMonth() + 1;
    const [monthlyRes, catRes, insightRes] = await Promise.all([
      API.get(`/analytics/monthly-comparison?year=${year}`),
      API.get(`/analytics/category-breakdown?year=${year}&month=${month}`),
      API.get('/analytics/insights'),
    ]);
    renderAnalytics(monthlyRes.data, catRes.data, insightRes.data, year, month);
  } catch(err) { page.innerHTML = `<div class="empty-state"><p>Failed to load: ${err.message}</p></div>`; }
}

function renderAnalytics(monthly, cats, insights, year, month) {
  const page = document.getElementById('analytics-page');
  const monthName = new Date(year, month-1).toLocaleDateString('en-IN',{month:'long',year:'numeric'});
  page.innerHTML = `
    <div class="page-header">
      <h2>Analytics 📈</h2>
      <select class="filter-select" id="year-select" onchange="changeAnalyticsYear(this.value)">
        ${[year-1, year, year+1].map(y => `<option value="${y}" ${y===year?'selected':''}>${y}</option>`).join('')}
      </select>
    </div>

    <!-- Line Chart — Full Year -->
    <div class="card" style="margin-bottom:16px">
      <div class="card-title"><span><i class="fas fa-chart-line" style="color:var(--accent)"></i> Income vs Expense — ${year}</span></div>
      <div class="chart-container" style="height:280px"><canvas id="analytics-line-chart"></canvas></div>
    </div>

    <!-- Category Breakdown -->
    <div class="charts-grid">
      <div class="card">
        <div class="card-title"><span><i class="fas fa-chart-bar" style="color:var(--accent)"></i> Category Spending — ${monthName}</span></div>
        <div class="chart-container" style="height:260px"><canvas id="analytics-cat-chart"></canvas></div>
      </div>
      <div class="card">
        <div class="card-title"><span><i class="fas fa-list" style="color:var(--accent)"></i> Category Breakdown</span></div>
        <div id="cat-progress-list" style="max-height:260px;overflow-y:auto"></div>
      </div>
    </div>

    <!-- Insights -->
    <div class="card" style="margin-top:16px">
      <div class="card-title"><span><i class="fas fa-robot" style="color:var(--accent)"></i> AI-Based Expense Suggestions</span></div>
      <div id="analytics-insights" class="insights-list"></div>
    </div>
  `;

  renderLineChart(monthly);
  renderCatChart(cats);
  renderCatProgress(cats);
  renderInsights2(insights, 'analytics-insights');
}

function renderLineChart(monthly) {
  const ctx = document.getElementById('analytics-line-chart');
  if (!ctx) return;
  if (lineChart) lineChart.destroy();
  const isDark = document.body.classList.contains('dark-mode');
  lineChart = new Chart(ctx, {
    type: 'line',
    data: {
      labels: monthly.labels || [],
      datasets: [
        {
          label: 'Income',
          data: monthly.income || [],
          borderColor: '#10b981', backgroundColor: 'rgba(16,185,129,0.1)',
          tension: 0.4, fill: true, pointBackgroundColor: '#10b981', pointRadius: 4
        },
        {
          label: 'Expense',
          data: monthly.expense || [],
          borderColor: '#ef4444', backgroundColor: 'rgba(239,68,68,0.08)',
          tension: 0.4, fill: true, pointBackgroundColor: '#ef4444', pointRadius: 4
        },
        {
          label: 'Balance',
          data: monthly.balance || [],
          borderColor: '#6366f1', backgroundColor: 'transparent',
          tension: 0.4, borderDash: [6,3], pointRadius: 3
        }
      ]
    },
    options: {
      responsive: true, maintainAspectRatio: false,
      interaction: { mode: 'index', intersect: false },
      scales: {
        x: { grid: { display: false } },
        y: { grid: { color: isDark?'rgba(255,255,255,0.05)':'rgba(0,0,0,0.05)' },
             ticks: { callback: v => App.currency + v.toLocaleString() } }
      },
      plugins: { legend: { position: 'top', labels: { usePointStyle: true } },
                 tooltip: { callbacks: { label: ctx => ` ${ctx.dataset.label}: ${formatCurrency(ctx.raw)}` } } }
    }
  });
}

function renderCatChart(cats) {
  const ctx = document.getElementById('analytics-cat-chart');
  if (!ctx) return;
  if (catBarChart) catBarChart.destroy();
  const catData = cats.categories || [];
  catBarChart = new Chart(ctx, {
    type: 'bar',
    data: {
      labels: catData.map(c => c.name),
      datasets: [{
        label: 'Amount',
        data: catData.map(c => c.amount),
        backgroundColor: ['#6366f1','#8b5cf6','#ec4899','#ef4444','#f97316','#f59e0b','#10b981','#06b6d4'],
        borderRadius: 8, borderSkipped: false
      }]
    },
    options: {
      responsive: true, maintainAspectRatio: false, indexAxis: 'y',
      scales: { x: { ticks: { callback: v => App.currency + v.toLocaleString() } }, y: { grid: { display: false } } },
      plugins: { legend: { display: false }, tooltip: { callbacks: { label: ctx => ` ${formatCurrency(ctx.raw)}` } } }
    }
  });
}

function renderCatProgress(cats) {
  const el = document.getElementById('cat-progress-list');
  if (!el) return;
  const catData = (cats.categories || []).slice(0, 10);
  if (!catData.length) { el.innerHTML = '<div class="empty-state" style="padding:20px"><p>No expenses this month</p></div>'; return; }
  el.innerHTML = catData.map(c => `
    <div style="margin-bottom:14px">
      <div style="display:flex;justify-content:space-between;margin-bottom:5px;font-size:13px">
        <span style="color:var(--text-primary);font-weight:500">${c.name}</span>
        <span style="color:var(--text-secondary)">${formatCurrency(c.amount)} <span style="color:var(--text-muted)">(${c.percentage}%)</span></span>
      </div>
      <div class="progress-bar">
        <div class="progress-fill safe" style="width:${Math.min(c.percentage,100)}%"></div>
      </div>
    </div>
  `).join('');
}

function renderInsights2(insights, containerId) {
  const el = document.getElementById(containerId);
  if (!el) return;
  el.innerHTML = (insights||[]).map(i => `
    <div class="insight-item ${i.type}">
      <div class="insight-icon"><i class="fas fa-${i.icon}"></i></div>
      <div class="insight-content"><h4>${i.title}</h4><p>${i.message}</p></div>
    </div>
  `).join('') || '<p style="color:var(--text-muted);padding:12px">No insights available.</p>';
}

async function changeAnalyticsYear(year) {
  const now = new Date();
  const month = now.getMonth() + 1;
  try {
    const [monthlyRes, catRes] = await Promise.all([
      API.get(`/analytics/monthly-comparison?year=${year}`),
      API.get(`/analytics/category-breakdown?year=${year}&month=${month}`)
    ]);
    renderLineChart(monthlyRes.data);
    renderCatChart(catRes.data);
    renderCatProgress(catRes.data);
  } catch(err) { showToast('Failed to load year data', 'error'); }
}
