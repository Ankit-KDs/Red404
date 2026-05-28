// ============================================================
// donor.js — Red404 Donor Dashboard Logic
// Depends on: auth.js (Auth, API, showAlert, setButtonLoading, Validate, escHtml, formatDate)
// ============================================================

// ── Auth guard (uses auth.js) ─────────────────────────────────
Auth.requireLogin('DONOR');

// ── Init ──────────────────────────────────────────────────────
document.addEventListener('DOMContentLoaded', () => {
  const { name } = Auth.getUser();
  setName(name);
  loadStats();
  loadRequests();
  loadProfile();
});

function setName(name) {
  document.getElementById('nav-name').textContent   = name;
  document.getElementById('sidebar-name').textContent = name;
  document.getElementById('welcome-name').textContent = `Welcome, ${name}!`;
  document.getElementById('avatar-letter').textContent = name.charAt(0).toUpperCase();
  sessionStorage.setItem('userName', name); // keep session in sync
}

// ── Tab Navigation ────────────────────────────────────────────
function show(section, el) {
  document.querySelectorAll('.tab-section').forEach(s => s.classList.remove('active'));
  document.querySelectorAll('.sidebar-nav a').forEach(a => a.classList.remove('active'));
  document.getElementById('sec-' + section).classList.add('active');
  if (el) el.classList.add('active');
  if (section === 'requests') loadRequests();
}

// ── Load Stats ────────────────────────────────────────────────
async function loadStats() {
  try {
    const { ok, data } = await API.get('/api/donor/stats');
    if (!ok) return;
    document.getElementById('stat-fulfilled').textContent = data.TOTAL_FULFILLED  || 0;
    document.getElementById('stat-pending').textContent   = data.PENDING          || 0;
    document.getElementById('stat-accepted').textContent  = data.ACCEPTED         || 0;
    document.getElementById('stat-rejected').textContent  = data.REJECTED         || 0;
  } catch(e) { console.error(e); }
}

// ── Load Requests ─────────────────────────────────────────────
async function loadRequests() {
  try {
    const { ok, data: requests } = await API.get('/api/donor/requests');
    if (!ok) { document.getElementById('requests-list').innerHTML = '<p class="alert alert-error">Failed to load requests.</p>'; return; }

    const tableHTML = requests.length === 0
      ? '<p style="color:var(--gray-500);text-align:center;padding:2rem;">No pending requests at the moment.</p>'
      : `<table>
          <thead><tr>
            <th>Hospital</th><th>Blood</th><th>Units</th><th>Urgency</th>
            <th>Distance</th><th>Message</th><th>Received</th><th>Action</th>
          </tr></thead>
          <tbody>
            ${requests.map(r => `
              <tr>
                <td><strong>${escHtml(r.hospitalName || '—')}</strong></td>
                <td><span class="blood-badge">${escHtml(r.bloodGroup)}</span></td>
                <td style="text-align:center;">${r.unitsNeeded}</td>
                <td><span class="urgency-${r.urgency}">${r.urgency}</span></td>
                <td><span class="dist-chip">${r.distanceKm ? r.distanceKm.toFixed(1) + ' km' : '—'}</span></td>
                <td style="max-width:180px;font-size:.85rem;">${escHtml(r.message || '—')}</td>
                <td style="font-size:.8rem;">${formatDate(r.requestedAt)}</td>
                <td>
                  <button class="btn btn-success btn-sm" onclick="respond(${r.id},'ACCEPTED')">✓ Accept</button>
                  <button class="btn btn-danger btn-sm" onclick="respond(${r.id},'REJECTED')" style="margin-top:4px;">✗ Decline</button>
                </td>
              </tr>
            `).join('')}
          </tbody>
        </table>`;

    document.getElementById('requests-list').innerHTML          = tableHTML;
    document.getElementById('overview-requests-wrap').innerHTML = tableHTML;
  } catch(e) {
    document.getElementById('requests-list').innerHTML = '<p class="alert alert-error">Failed to load requests.</p>';
  }
}

// ── Respond to Request ────────────────────────────────────────
async function respond(requestId, status) {
  try {
    const { ok, data } = await API.post('/api/donor/requests/respond', { requestId, status });
    if (ok) {
      showAlert('alert-requests', `Response recorded: ${status}`, 'success');
      loadRequests(); loadStats();
    } else {
      showAlert('alert-requests', data.error || 'Failed to respond.', 'error');
    }
  } catch(e) { console.error(e); }
}

// ── Load Profile ──────────────────────────────────────────────
async function loadProfile() {
  try {
    const { ok, data } = await API.get('/api/donor/profile');
    if (!ok) return;
    document.getElementById('p-name').value      = data.name      || '';
    document.getElementById('p-phone').value     = data.phone     || '';
    document.getElementById('p-address').value   = data.address   || '';
    document.getElementById('p-lat').value       = data.latitude  || '';
    document.getElementById('p-lon').value       = data.longitude || '';
    document.getElementById('p-available').value = data.available ? 'true' : 'false';
  } catch(e) { console.error(e); }
}

// ── Update Profile ────────────────────────────────────────────
async function updateProfile(e) {
  e.preventDefault();
  setButtonLoading('profile-btn', true, 'Save Changes');
  try {
    const payload = {
      name:      document.getElementById('p-name').value,
      phone:     document.getElementById('p-phone').value,
      address:   document.getElementById('p-address').value,
      latitude:  parseFloat(document.getElementById('p-lat').value)  || 0,
      longitude: parseFloat(document.getElementById('p-lon').value) || 0,
      available: document.getElementById('p-available').value === 'true'
    };
    const { ok, data } = await API.put('/api/donor/profile', payload);
    if (ok) {
      setName(data.name);
      showAlert('alert-profile', 'Profile updated successfully!', 'success');
    } else {
      showAlert('alert-profile', data.error || 'Update failed.', 'error');
    }
  } catch(e) {
    showAlert('alert-profile', 'Network error.', 'error');
  } finally {
    setButtonLoading('profile-btn', false, 'Save Changes');
  }
}

// ── Change Password ───────────────────────────────────────────
async function changePassword(e) {
  e.preventDefault();
  const newPw  = document.getElementById('pw-new').value;
  const confPw = document.getElementById('pw-confirm').value;
  if (!Validate.passwordsMatch(newPw, confPw)) {
    showAlert('alert-password', 'Passwords do not match.', 'error'); return;
  }
  setButtonLoading('pw-btn', true, 'Update Password');
  try {
    const { ok, data } = await API.put('/api/donor/password', {
      currentPassword: document.getElementById('pw-current').value,
      newPassword: newPw
    });
    if (ok) {
      showAlert('alert-password', 'Password updated successfully!', 'success');
      ['pw-current','pw-new','pw-confirm'].forEach(id => document.getElementById(id).value = '');
    } else {
      showAlert('alert-password', data.error || 'Failed to update password.', 'error');
    }
  } catch(e) {
    showAlert('alert-password', 'Network error.', 'error');
  } finally {
    setButtonLoading('pw-btn', false, 'Update Password');
  }
}

// ── Logout (uses auth.js shared logout) ───────────────────────
// logout() is defined in auth.js — no need to redefine here