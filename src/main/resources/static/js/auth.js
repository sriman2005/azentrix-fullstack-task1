/* =============================================
   Auth JS — Login, Register, Password Reset
   ============================================= */

// ==================== LOGIN ====================
document.getElementById('login-form').addEventListener('submit', async (e) => {
  e.preventDefault();
  const btn = document.getElementById('login-btn');
  const errEl = document.getElementById('login-error');
  errEl.classList.add('hidden');
  btn.disabled = true;
  btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Signing in...';

  try {
    const data = await API.request('POST', '/auth/login', {
      email: document.getElementById('login-email').value.trim(),
      password: document.getElementById('login-password').value,
    });
    saveAuth(data.data);
    showAppSection();
    navigateTo('dashboard');
    showToast(`Welcome back, ${App.user.fullName}! 👋`, 'success');
  } catch (err) {
    errEl.textContent = err.message || 'Invalid email or password';
    errEl.classList.remove('hidden');
  } finally {
    btn.disabled = false;
    btn.innerHTML = '<span>Sign In</span><i class="fas fa-arrow-right"></i>';
  }
});

// ==================== REGISTER ====================
document.getElementById('reg-password').addEventListener('input', (e) => {
  const val = e.target.value;
  const el = document.getElementById('password-strength');
  let strength = 0;
  if (val.length >= 8) strength += 25;
  if (/[A-Z]/.test(val)) strength += 25;
  if (/[0-9]/.test(val)) strength += 25;
  if (/[^A-Za-z0-9]/.test(val)) strength += 25;
  const color = strength <= 25 ? '#ef4444' : strength <= 50 ? '#f97316' : strength <= 75 ? '#f59e0b' : '#10b981';
  el.style.setProperty('--strength', strength + '%');
  el.style.setProperty('--strength-color', color);
});

document.getElementById('register-form').addEventListener('submit', async (e) => {
  e.preventDefault();
  const btn = document.getElementById('register-btn');
  const errEl = document.getElementById('register-error');
  errEl.classList.add('hidden');
  btn.disabled = true;
  btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Creating account...';

  try {
    const data = await API.request('POST', '/auth/register', {
      fullName: document.getElementById('reg-name').value.trim(),
      email: document.getElementById('reg-email').value.trim(),
      password: document.getElementById('reg-password').value,
      phone: document.getElementById('reg-phone').value.trim() || null,
    });
    saveAuth(data.data);
    showAppSection();
    navigateTo('dashboard');
    showToast('🎉 Account created! Welcome to Budget Tracker!', 'success');
  } catch (err) {
    // Show detailed validation errors if available
    let msg = err.message || 'Registration failed';
    // If the server returned field-level errors (object), list them
    try {
      const parsed = JSON.parse(err.message);
      if (typeof parsed === 'object') {
        msg = Object.values(parsed).join(' • ');
      }
    } catch(_) {}
    errEl.textContent = msg;
    errEl.classList.remove('hidden');
  } finally {
    btn.disabled = false;
    btn.innerHTML = '<span>Create Account</span><i class="fas fa-arrow-right"></i>';
  }
});

// ==================== FORGOT PASSWORD ====================
document.getElementById('forgot-form').addEventListener('submit', async (e) => {
  e.preventDefault();
  const btn = document.getElementById('forgot-btn');
  const msgEl = document.getElementById('forgot-msg');
  msgEl.className = 'hidden';
  btn.disabled = true;
  btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Sending...';

  try {
    await API.request('POST', '/auth/forgot-password', {
      email: document.getElementById('forgot-email').value.trim()
    });
    msgEl.className = 'success-message';
    msgEl.innerHTML = '✅ Reset link sent! Check your email inbox (and spam folder).';
  } catch (err) {
    msgEl.className = 'error-message';
    msgEl.textContent = err.message || 'Failed to send reset email';
  } finally {
    btn.disabled = false;
    btn.innerHTML = '<span>Send Reset Link</span><i class="fas fa-paper-plane"></i>';
  }
});

// ==================== RESET PASSWORD ====================
document.getElementById('reset-form').addEventListener('submit', async (e) => {
  e.preventDefault();
  const msgEl = document.getElementById('reset-msg');
  msgEl.className = 'hidden';

  try {
    await API.request('POST', '/auth/reset-password', {
      token: window.pendingResetToken,
      newPassword: document.getElementById('reset-password').value
    });
    msgEl.className = 'success-message';
    msgEl.textContent = '✅ Password reset! Redirecting to login...';
    setTimeout(() => {
      window.history.replaceState({}, '', '/');
      showPage('login-page');
    }, 2000);
  } catch (err) {
    msgEl.className = 'error-message';
    msgEl.textContent = err.message || 'Password reset failed';
  }
});
