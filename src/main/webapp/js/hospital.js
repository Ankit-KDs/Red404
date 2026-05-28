// ============================================================
// hospital.js — Red404 Hospital Dashboard Logic
// Depends on: auth.js (Auth, API, showAlert, setButtonLoading,
//             Validate, escHtml, escAttr, formatDate, logout)
// ============================================================

// ── Auth guard ────────────────────────────────────────────────
Auth.requireLogin('HOSPITAL');

// ── Init ──────────────────────────────────────────────────────
document.addEventListener('DOMContentLoaded', () => {
  const { name } = Auth.getUser();
  setName(name);
  loadStats();
  loadMyRequests();
  loadProfile();
});

function setName(name) {
  document.getElementById('nav-name').textContent      = name;
  document.getElementById('sidebar-name').textContent  = name;
  document.getElementById('welcome-name').textContent  = name;
  document.getElementById('avatar-letter').textContent = name.charAt(0).toUpperCase();
  sessionStorage.setItem('userName', name);
}

// ── Tab Navigation ────────────────────────────────────────────
function show(section, el) {
  document.querySelectorAll('.tab-section').forEach(s => s.classList.remove('active'));
  document.querySelectorAll('.sidebar-nav a').forEach(a => a.classList.remove('active'));
  document.getElementById('sec-' + section).classList.add('active');
  if (el) el.classList.add('active');
  if (section === 'requests') loadMyRequests();
}

// ── Load Stats ────────────────────────────────────────────────
async function loadStats() {
  try {
    const { ok, data } = await API.get('/api/hospital/stats');
    if (!ok) return;
    document.getElementById('stat-pending').textContent   = data.PENDING   || 0;
    document.getElementById('stat-accepted').textContent  = data.ACCEPTED  || 0;
    document.getElementById('stat-fulfilled').textContent = data.FULFILLED || 0;
    document.getElementById('stat-rejected').textContent  = data.REJECTED  || 0;
  } catch(e) { console.error(e); }
}

// ── Search Nearby Donors ──────────────────────────────────────
async function searchDonors() {
  const bg     = document.getElementById('search-bg').value;
  const radius = document.getElementById('search-radius').value;
  let url = `/api/hospital/donors/nearby?radius=${radius}`;
  if (bg) url += `&bg=${encodeURIComponent(bg)}`;
  renderDonors(url, `Nearby donors within ${radius} km${bg ? ' with ' + bg : ''}`);
}

async function loadAllDonors() {
  renderDonors('/api/hospital/donors', 'All Registered Donors');
}

async function renderDonors(url, title) {
  document.getElementById('donors-table-wrap').innerHTML = '<p style="color:var(--gray-500);">Loading...</p>';
  document.getElementById('alert-donors').innerHTML      = '';
  try {
    const { ok, data: donors } = await API.get(url);
    if (!ok) { showAlert('alert-donors', donors.error || 'Failed to load donors.', 'error'); return; }

    if (donors.length === 0) {
      document.getElementById('donors-table-wrap').innerHTML =
        '<p style="color:var(--gray-500);text-align:center;padding:2rem;">No donors found in this zone for the selected blood group.</p>';
      return;
    }
    document.getElementById('donors-table-wrap').innerHTML = `
      <div style="margin-bottom:.8rem;font-size:.85rem;color:var(--gray-500);">
        <strong>${donors.length}</strong> donor(s) found — ${title}
      </div>
      <table>
        <thead><tr>
          <th>Name</th><th>Blood Group</th><th>Phone</th><th>Address</th>
          <th>Distance</th><th>Available</th><th>Action</th>
        </tr></thead>
        <tbody>
          ${donors.map(d => `
            <tr>
              <td><strong>${escHtml(d.name)}</strong></td>
              <td><span class="blood-badge">${escHtml(d.bloodGroup)}</span></td>
              <td>${escHtml(d.phone)}</td>
              <td style="font-size:.85rem;">${escHtml(d.address)}</td>
              <td><span class="dist-chip">${d.distanceKm} km</span></td>
              <td>
                ${d.available
                  ? '<span class="badge badge-green">Available</span>'
                  : '<span class="badge badge-gray">Unavailable</span>'}
              </td>
              <td>
                <button class="btn btn-primary btn-sm"
                  onclick="openModal(${d.id},'${escAttr(d.name)}','${escAttr(d.bloodGroup)}')"
                  ${d.available ? '' : 'disabled'}>
                  🩸 Request
                </button>
              </td>
            </tr>
          `).join('')}
        </tbody>
      </table>`;
  } catch(e) {
    showAlert('alert-donors', 'Network error. Could not load donors.', 'error');
  }
}

// ── Request Modal ─────────────────────────────────────────────
function openModal(donorId, donorName, bloodGroup) {
  document.getElementById('modal-donor-id').value   = donorId;
  document.getElementById('modal-donor-name').value = donorName;
  document.getElementById('modal-bg').value          = bloodGroup;
  document.getElementById('modal-message').value     = '';
  document.getElementById('modal-units').value       = 1;
  document.getElementById('modal-urgency').value     = 'NORMAL';
  document.getElementById('alert-modal').innerHTML   = '';
  document.getElementById('request-modal').classList.add('show');
}
function closeModal() {
  document.getElementById('request-modal').classList.remove('show');
}

// ── Send Blood Request ────────────────────────────────────────
async function sendRequest() {
  setButtonLoading('send-btn', true, '🩸 Send Request');
  try {
    const { ok, data } = await API.post('/api/hospital/request', {
      donorId:     parseInt(document.getElementById('modal-donor-id').value),
      bloodGroup:  document.getElementById('modal-bg').value,
      unitsNeeded: parseInt(document.getElementById('modal-units').value),
      urgency:     document.getElementById('modal-urgency').value,
      message:     document.getElementById('modal-message').value
    });
    if (ok) {
      showAlert('alert-modal', 'Request sent successfully!', 'success');
      loadStats(); loadMyRequests();
      setTimeout(closeModal, 1500);
    } else {
      showAlert('alert-modal', data.error || 'Failed to send request.', 'error');
    }
  } catch(e) {
    showAlert('alert-modal', 'Network error.', 'error');
  } finally {
    setButtonLoading('send-btn', false, '🩸 Send Request');
  }
}

// ── My Requests ───────────────────────────────────────────────
async function loadMyRequests() {
  try {
    const { ok, data: requests } = await API.get('/api/hospital/requests');
    if (!ok) return;
    const statusBadge = s => {
      const map = { PENDING:'badge-yellow', ACCEPTED:'badge-green', REJECTED:'badge-gray', FULFILLED:'badge-blue', EXPIRED:'badge-gray' };
      return `<span class="badge ${map[s]||'badge-gray'}">${s}</span>`;
    };
    const tableHTML = requests.length === 0
      ? '<p style="color:var(--gray-500);text-align:center;padding:2rem;">No requests sent yet.</p>'
      : `<table>
          <thead><tr>
            <th>Donor</th><th>Blood</th><th>Units</th><th>Urgency</th>
            <th>Distance</th><th>Status</th><th>Sent</th><th>Responded</th>
          </tr></thead>
          <tbody>
            ${requests.map(r => `
              <tr>
                <td>${escHtml(r.donorName || '—')}</td>
                <td><span class="blood-badge">${escHtml(r.bloodGroup)}</span></td>
                <td style="text-align:center;">${r.unitsNeeded}</td>
                <td><span class="urgency-${r.urgency}">${r.urgency}</span></td>
                <td><span class="dist-chip">${r.distanceKm ? r.distanceKm.toFixed(1) + ' km' : '—'}</span></td>
                <td>${statusBadge(r.status)}</td>
                <td style="font-size:.8rem;">${formatDate(r.requestedAt)}</td>
                <td style="font-size:.8rem;">${r.respondedAt ? formatDate(r.respondedAt) : '—'}</td>
              </tr>
            `).join('')}
          </tbody>
        </table>`;
    document.getElementById('my-requests-wrap').innerHTML        = tableHTML;
    document.getElementById('overview-requests-wrap').innerHTML  = tableHTML;
  } catch(e) { console.error(e); }
}

// ── Load Profile ──────────────────────────────────────────────
async function loadProfile() {
  try {
    const { ok, data } = await API.get('/api/hospital/profile');
    if (!ok) return;
    document.getElementById('p-name').value       = data.name       || '';
    document.getElementById('p-phone').value      = data.phone      || '';
    document.getElementById('p-address').value    = data.address    || '';
    document.getElementById('p-adminname').value  = data.adminName  || '';
    document.getElementById('p-adminphone').value = data.adminPhone || '';
    document.getElementById('p-adminemail').value = data.adminEmail || '';
  } catch(e) { console.error(e); }
}

// ── Update Profile ────────────────────────────────────────────
async function updateProfile(e) {
  e.preventDefault();
  setButtonLoading('profile-btn', true, 'Save Changes');
  try {
    const payload = {
      name:       document.getElementById('p-name').value,
      phone:      document.getElementById('p-phone').value,
      address:    document.getElementById('p-address').value,
      adminName:  document.getElementById('p-adminname').value,
      adminPhone: document.getElementById('p-adminphone').value,
      adminEmail: document.getElementById('p-adminemail').value
    };
    const { ok, data } = await API.put('/api/hospital/profile', payload);
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
  const newPw = document.getElementById('pw-new').value;
  if (!Validate.passwordsMatch(newPw, document.getElementById('pw-confirm').value)) {
    showAlert('alert-password', 'Passwords do not match.', 'error'); return;
  }
  setButtonLoading('pw-btn', true, 'Update Password');
  try {
    const { ok, data } = await API.put('/api/hospital/password', {
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

// logout() provided globally by auth.js