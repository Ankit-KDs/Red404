// ============================================================
// auth.js — Red404 Shared Authentication Utilities
// Used by: login.html, register-donor.html, register-hospital.html
// ============================================================

// ── Session Helpers ───────────────────────────────────────────
const Auth = {

  // Save session after login/register
  saveSession(data) {
    sessionStorage.setItem('userId',   data.id);
    sessionStorage.setItem('userName', data.name);
    sessionStorage.setItem('userType', data.type);
  },

  // Clear session on logout
  clearSession() {
    sessionStorage.clear();
  },

  // Get current logged-in user info
  getUser() {
    return {
      id:   sessionStorage.getItem('userId'),
      name: sessionStorage.getItem('userName'),
      type: sessionStorage.getItem('userType')
    };
  },

  // Check if user is logged in
  isLoggedIn() {
    return !!sessionStorage.getItem('userId');
  },

  // Redirect if already logged in (used on login/register pages)
  redirectIfLoggedIn() {
    if (!this.isLoggedIn()) return;
    const type = sessionStorage.getItem('userType');
    window.location.href = type === 'DONOR' ? 'donor-dashboard.html' : 'hospital-dashboard.html';
  },

  // Redirect to login if NOT logged in (used on dashboard pages)
  requireLogin(expectedType = null) {
    if (!this.isLoggedIn()) {
      window.location.href = 'login.html';
      return false;
    }
    if (expectedType && sessionStorage.getItem('userType') !== expectedType) {
      window.location.href = 'login.html';
      return false;
    }
    return true;
  }
};

// ── API Helper ────────────────────────────────────────────────
const API = {

  // Generic POST request
  async post(url, payload) {
    const res  = await fetch(url, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });
    const data = await res.json();
    return { ok: res.ok, status: res.status, data };
  },

  // Generic PUT request
  async put(url, payload) {
    const res  = await fetch(url, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });
    const data = await res.json();
    return { ok: res.ok, status: res.status, data };
  },

  // Generic GET request
  async get(url) {
    const res  = await fetch(url);
    const data = await res.json();
    return { ok: res.ok, status: res.status, data };
  }
};

// ── Alert Helper ──────────────────────────────────────────────
/**
 * showAlert(containerId, message, type)
 * type: 'success' | 'error' | 'info'
 */
function showAlert(containerId, message, type = 'info') {
  const el = document.getElementById(containerId);
  if (!el) return;
  el.innerHTML = `<div class="alert alert-${type}">${escapeHtml(message)}</div>`;
  el.scrollIntoView({ behavior: 'smooth', block: 'nearest' });

  // Auto-clear success alerts after 4 seconds
  if (type === 'success') {
    setTimeout(() => { el.innerHTML = ''; }, 4000);
  }
}

// ── Button Loading State ──────────────────────────────────────
function setButtonLoading(btnId, loading, defaultText) {
  const btn = document.getElementById(btnId);
  if (!btn) return;
  if (loading) {
    btn.innerHTML = '<span class="loader"></span> Please wait...';
    btn.disabled  = true;
  } else {
    btn.innerHTML = defaultText;
    btn.disabled  = false;
  }
}

// ── Input Validation Helpers ──────────────────────────────────
const Validate = {

  // Email format check
  email(value) {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value);
  },

  // Phone: exactly 10 digits
  phone(value) {
    return /^[0-9]{10}$/.test(value);
  },

  // Password: min 8 chars
  password(value) {
    return value && value.length >= 8;
  },

  // Passwords match
  passwordsMatch(pw, confirm) {
    return pw === confirm;
  },

  // Age within donation range
  donorAge(age) {
    return age >= 18 && age <= 65;
  },

  // Non-empty string
  required(value) {
    return value && value.trim().length > 0;
  },

  // Validate full donor registration form; returns array of error strings
  donorForm(fields) {
    const errors = [];
    if (!this.required(fields.name))           errors.push('Full name is required.');
    if (!this.donorAge(fields.age))            errors.push('Age must be between 18 and 65.');
    if (!this.required(fields.dob))            errors.push('Date of birth is required.');
    if (!this.required(fields.bloodGroup))     errors.push('Blood group is required.');
    if (!this.phone(fields.phone))             errors.push('Enter a valid 10-digit phone number.');
    if (!this.email(fields.email))             errors.push('Enter a valid email address.');
    if (!this.required(fields.address))        errors.push('Address is required.');
    if (!this.password(fields.password))       errors.push('Password must be at least 8 characters.');
    if (!this.passwordsMatch(fields.password, fields.confirm)) errors.push('Passwords do not match.');
    return errors;
  },

  // Validate hospital registration form
  hospitalForm(fields) {
    const errors = [];
    if (!this.required(fields.name))           errors.push('Hospital name is required.');
    if (!this.required(fields.registrationId)) errors.push('Registration ID is required.');
    if (!this.required(fields.phone))          errors.push('Hospital phone is required.');
    if (!this.email(fields.email))             errors.push('Enter a valid hospital email.');
    if (!this.required(fields.address))        errors.push('Hospital address is required.');
    if (!this.required(fields.adminName))      errors.push('Admin name is required.');
    if (!this.phone(fields.adminPhone))        errors.push('Enter a valid 10-digit admin phone.');
    if (!this.email(fields.adminEmail))        errors.push('Enter a valid admin email.');
    if (!this.password(fields.password))       errors.push('Password must be at least 8 characters.');
    if (!this.passwordsMatch(fields.password, fields.confirm)) errors.push('Passwords do not match.');
    return errors;
  }
};

// ── Geolocation Helper ────────────────────────────────────────
function detectLocation(latFieldId, lonFieldId, btnEl) {
  if (!navigator.geolocation) {
    alert('Geolocation is not supported by your browser.');
    return;
  }
  if (btnEl) { btnEl.textContent = '📍 Detecting...'; btnEl.disabled = true; }
  navigator.geolocation.getCurrentPosition(
    pos => {
      document.getElementById(latFieldId).value = pos.coords.latitude.toFixed(6);
      document.getElementById(lonFieldId).value = pos.coords.longitude.toFixed(6);
      if (btnEl) { btnEl.textContent = '✅ Location Detected'; btnEl.disabled = false; }
    },
    err => {
      alert('Could not detect location. Please enter coordinates manually.\nError: ' + err.message);
      if (btnEl) { btnEl.textContent = '📍 Auto-detect My Location'; btnEl.disabled = false; }
    }
  );
}

// ── Logout (shared by both dashboards) ───────────────────────
async function logout() {
  try {
    await fetch('/api/auth/logout', { method: 'POST' });
  } catch(e) {
    // ignore network errors on logout
  } finally {
    Auth.clearSession();
    window.location.href = 'login.html';
  }
}

// ── Escape HTML (XSS prevention) ─────────────────────────────
function escapeHtml(str) {
  return String(str)
    .replace(/&/g,  '&amp;')
    .replace(/</g,  '&lt;')
    .replace(/>/g,  '&gt;')
    .replace(/"/g,  '&quot;')
    .replace(/'/g,  '&#39;');
}

// Alias used in donor.js and hospital.js
function escHtml(str)  { return escapeHtml(str); }
function escAttr(str)  { return escapeHtml(str); }

// ── Date Formatter (shared) ───────────────────────────────────
function formatDate(ts) {
  if (!ts) return '—';
  return ts;
}

// ── On page load: redirect if already logged in ───────────────
// (only runs on login/register pages — dashboards won't call this)
// if (document.body.classList.contains('auth-page')) {
//   Auth.redirectIfLoggedIn();
// }

// ── On page load: redirect if already logged in ───────────────
try {
  if (document.body && document.body.classList.contains('auth-page')) {
    Auth.redirectIfLoggedIn();
  }
} catch(e) {}