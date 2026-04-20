if (!checkAuth('RECEPTION')) { /* redirected */ }
document.getElementById('sidebar-name').textContent = getUserName();

function showTab(id) {
    document.querySelectorAll('.main-content > div[id^="tab-"]').forEach(el => el.classList.add('d-none'));
    document.getElementById(id)?.classList.remove('d-none');
    const loaders = { 'tab-clients': loadAllClients, 'tab-visits': loadTodayVisits, 'tab-active': loadActiveMemberships, 'tab-membership': initMembershipForm };
    if (loaders[id]) loaders[id]();
}

async function loadAllClients() {
    try {
        const clients = await get('/reception/clients');
        renderClients(clients);
    } catch (e) { showErr('alert-err', e.message); }
}

async function searchClients() {
    const q = document.getElementById('client-search').value;
    try {
        const clients = await get('/reception/clients?query=' + encodeURIComponent(q));
        renderClients(clients);
    } catch (e) { showErr('alert-err', e.message); }
}

function renderClients(clients) {
    document.getElementById('clients-table').innerHTML = clients.map(c => `
        <tr>
            <td>${c.firstName} ${c.lastName}</td>
            <td>${c.email}</td>
            <td>${c.phone || '—'}</td>
            <td><span class="badge ${c.hasActiveMembership ? 'bg-success' : 'bg-danger'}">${c.hasActiveMembership ? 'Активен' : 'Нет'}</span></td>
            <td>
                <button class="btn btn-sm btn-outline-primary" onclick="quickMarkVisit(${c.id})">✅ Визит</button>
                <button class="btn btn-sm btn-outline-secondary ms-1" onclick="prefillMembership(${c.id}, '${c.email}')">💳 Абонемент</button>
            </td>
        </tr>`).join('');
}

async function quickMarkVisit(clientId) {
    try {
        await post('/reception/visits', { clientId });
        showOk('alert-ok', 'Посещение отмечено');
    } catch (e) { showErr('alert-err', e.message); }
}

function prefillMembership(clientId, email) {
    showTab('tab-membership');
    document.getElementById('mem-client-id').value = clientId;
    document.getElementById('mem-client-search').value = email;
}

document.getElementById('new-client-form')?.addEventListener('submit', async e => {
    e.preventDefault();
    try {
        await post('/reception/clients', {
            firstName: document.getElementById('nc-first').value,
            lastName: document.getElementById('nc-last').value,
            email: document.getElementById('nc-email').value,
            phone: document.getElementById('nc-phone').value,
            password: document.getElementById('nc-password').value,
            birthDate: document.getElementById('nc-birth').value || null
        });
        showOk('alert-ok', 'Клиент создан');
        e.target.reset();
    } catch (e2) { showErr('alert-err', e2.message); }
});

async function initMembershipForm() {
    try {
        const types = await get('/client/memberships/types');
        const sel = document.getElementById('mem-type-select');
        sel.innerHTML = types.map(t => `<option value="${t.id}">${t.name} — ${t.price} ₽ (${t.durationDays} дн.)</option>`).join('');
        const today = new Date().toISOString().split('T')[0];
        document.getElementById('mem-start-date').value = today;
    } catch (e) { showErr('alert-err', e.message); }
}

document.getElementById('mem-client-search')?.addEventListener('input', async function() {
    const q = this.value;
    if (q.length < 2) return;
    try {
        const clients = await get('/reception/clients?query=' + encodeURIComponent(q));
        const sel = document.getElementById('mem-client-select');
        sel.style.display = clients.length ? 'block' : 'none';
        sel.innerHTML = clients.map(c => `<option value="${c.id}">${c.firstName} ${c.lastName} — ${c.email}</option>`).join('');
        sel.onchange = function() {
            document.getElementById('mem-client-id').value = this.value;
            sel.style.display = 'none';
        };
    } catch (e) {}
});

document.getElementById('membership-form')?.addEventListener('submit', async e => {
    e.preventDefault();
    const clientId = document.getElementById('mem-client-id').value;
    if (!clientId) { showErr('alert-err', 'Выберите клиента'); return; }
    try {
        await post('/reception/memberships', {
            clientId: Number(clientId),
            typeId: Number(document.getElementById('mem-type-select').value),
            startDate: document.getElementById('mem-start-date').value
        });
        showOk('alert-ok', 'Абонемент оформлен');
    } catch (e2) { showErr('alert-err', e2.message); }
});

async function searchForVisit() {
    const q = document.getElementById('visit-search').value;
    try {
        const clients = await get('/reception/clients?query=' + encodeURIComponent(q));
        document.getElementById('visit-client-list').innerHTML = clients.map(c => `
            <div class="d-flex align-items-center gap-3 mb-2 p-2 border rounded">
                <span>${c.firstName} ${c.lastName} (${c.email})</span>
                <span class="badge ${c.hasActiveMembership ? 'bg-success' : 'bg-danger'}">${c.hasActiveMembership ? 'Абонемент активен' : 'Нет абонемента'}</span>
                <button class="btn btn-sm btn-success" onclick="quickMarkVisit(${c.id})">✅ Отметить</button>
            </div>`).join('');
    } catch (e) { showErr('alert-err', e.message); }
}

async function loadTodayVisits() {
    try {
        const visits = await get('/reception/visits/today');
        document.getElementById('today-visits-table').innerHTML = visits.map(v => `
            <tr><td>${v.clientName}</td><td>${v.clientEmail}</td><td>${fmtDt(v.markedAt)}</td></tr>`).join('');
    } catch (e) { showErr('alert-err', e.message); }
}

async function loadActiveMemberships() {
    try {
        const memberships = await get('/reception/memberships/active');
        document.getElementById('active-table').innerHTML = memberships.map(m => `
            <tr><td>${m.clientName}</td><td>${m.typeName}</td><td>${fmtDate(m.startDate)}</td><td>${fmtDate(m.endDate)}</td><td>${m.paidAmount} ₽</td></tr>`).join('');
    } catch (e) { showErr('alert-err', e.message); }
}

loadAllClients();
