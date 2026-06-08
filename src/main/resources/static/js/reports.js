/* Reports JS */
function loadReports() {
  const page = document.getElementById('reports-page');
  page.innerHTML = `
    <div class="page-header"><h2>Export Reports 📊</h2></div>

    <div class="card" style="margin-bottom:24px">
      <div class="card-title"><span><i class="fas fa-calendar-alt" style="color:var(--accent)"></i> Select Date Range</span></div>
      <div style="display:flex;gap:16px;flex-wrap:wrap;align-items:flex-end">
        <div class="form-group" style="flex:1;min-width:150px;margin:0">
          <label style="font-size:13px;color:var(--text-secondary);display:block;margin-bottom:6px">Start Date</label>
          <input type="date" id="export-start" style="width:100%;padding:10px 14px;background:var(--bg-tertiary);border:1.5px solid var(--border);border-radius:10px;color:var(--text-primary);font-size:14px"/>
        </div>
        <div class="form-group" style="flex:1;min-width:150px;margin:0">
          <label style="font-size:13px;color:var(--text-secondary);display:block;margin-bottom:6px">End Date</label>
          <input type="date" id="export-end" value="${todayISO()}" style="width:100%;padding:10px 14px;background:var(--bg-tertiary);border:1.5px solid var(--border);border-radius:10px;color:var(--text-primary);font-size:14px"/>
        </div>
        <div style="display:flex;gap:8px;flex-wrap:wrap">
          <button class="btn-secondary" style="padding:10px 16px;font-size:13px" onclick="setPreset('thisMonth')">This Month</button>
          <button class="btn-secondary" style="padding:10px 16px;font-size:13px" onclick="setPreset('lastMonth')">Last Month</button>
          <button class="btn-secondary" style="padding:10px 16px;font-size:13px" onclick="setPreset('thisYear')">This Year</button>
          <button class="btn-secondary" style="padding:10px 16px;font-size:13px" onclick="setPreset('all')">All Time</button>
        </div>
      </div>
    </div>

    <div class="export-options">
      <div class="export-card" onclick="exportReport('pdf')">
        <div class="export-icon">📄</div>
        <div class="export-title">PDF Report</div>
        <div class="export-desc">Download a beautifully formatted PDF with summary, charts, and transaction table</div>
        <button class="btn-primary" style="margin-top:16px;width:100%"><i class="fas fa-file-pdf"></i> Export PDF</button>
      </div>
      <div class="export-card" onclick="exportReport('csv')">
        <div class="export-icon">📊</div>
        <div class="export-title">CSV Spreadsheet</div>
        <div class="export-desc">Download raw transaction data as CSV for Excel or Google Sheets</div>
        <button class="btn-primary" style="margin-top:16px;width:100%;background:linear-gradient(135deg,#10b981,#059669)"><i class="fas fa-file-csv"></i> Export CSV</button>
      </div>
      <div class="export-card" onclick="exportReport('excel')">
        <div class="export-icon">📈</div>
        <div class="export-title">Excel Workbook</div>
        <div class="export-desc">Download a color-coded Excel file with transactions and summary sheets</div>
        <button class="btn-primary" style="margin-top:16px;width:100%;background:linear-gradient(135deg,#059669,#065f46)"><i class="fas fa-file-excel"></i> Export Excel</button>
      </div>
    </div>

    <div class="card" style="margin-top:24px">
      <div class="card-title"><span><i class="fas fa-info-circle" style="color:var(--accent)"></i> Report Tips</span></div>
      <ul style="list-style:none;display:flex;flex-direction:column;gap:10px">
        <li style="display:flex;align-items:center;gap:10px;font-size:14px;color:var(--text-secondary)">
          <i class="fas fa-check-circle" style="color:var(--income-color)"></i>
          PDF reports include a summary card and full transaction table
        </li>
        <li style="display:flex;align-items:center;gap:10px;font-size:14px;color:var(--text-secondary)">
          <i class="fas fa-check-circle" style="color:var(--income-color)"></i>
          CSV files can be imported into any spreadsheet application
        </li>
        <li style="display:flex;align-items:center;gap:10px;font-size:14px;color:var(--text-secondary)">
          <i class="fas fa-check-circle" style="color:var(--income-color)"></i>
          Excel files include color-coded income/expense and a summary sheet
        </li>
        <li style="display:flex;align-items:center;gap:10px;font-size:14px;color:var(--text-secondary)">
          <i class="fas fa-check-circle" style="color:var(--income-color)"></i>
          Leave date range empty to export all transactions
        </li>
      </ul>
    </div>
  `;
}

function setPreset(preset) {
  const now = new Date();
  let start = '', end = todayISO();
  if (preset === 'thisMonth') {
    start = `${now.getFullYear()}-${String(now.getMonth()+1).padStart(2,'0')}-01`;
  } else if (preset === 'lastMonth') {
    const last = new Date(now.getFullYear(), now.getMonth()-1, 1);
    const lastEnd = new Date(now.getFullYear(), now.getMonth(), 0);
    start = last.toISOString().split('T')[0];
    end = lastEnd.toISOString().split('T')[0];
  } else if (preset === 'thisYear') {
    start = `${now.getFullYear()}-01-01`;
  } else if (preset === 'all') {
    start = ''; end = '';
  }
  if (document.getElementById('export-start')) document.getElementById('export-start').value = start;
  if (document.getElementById('export-end')) document.getElementById('export-end').value = end;
}

async function exportReport(format) {
  const start = document.getElementById('export-start')?.value || '';
  const end = document.getElementById('export-end')?.value || '';
  let url = `/api/export/${format}`;
  const params = new URLSearchParams();
  if (start) params.set('startDate', start);
  if (end) params.set('endDate', end);
  if (params.toString()) url += '?' + params.toString();

  showToast(`Generating ${format.toUpperCase()} report...`, 'info');
  try {
    const res = await fetch(url, { headers: { 'Authorization': `Bearer ${App.token}` } });
    if (!res.ok) throw new Error('Export failed');
    const blob = await res.blob();
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    const ext = format === 'excel' ? 'xlsx' : format;
    const dateStr = start ? `${start}_to_${end||'now'}` : 'all';
    link.download = `budget_report_${dateStr}.${ext}`;
    link.click();
    showToast(`${format.toUpperCase()} downloaded! ✅`, 'success');
  } catch(err) {
    showToast('Export failed: ' + err.message, 'error');
  }
}
