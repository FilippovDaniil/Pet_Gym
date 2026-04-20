if (!checkAuth('ADMIN')) { /* redirected */ }
document.getElementById('sidebar-name').textContent = getUserName();

function showTab(id) {
    document.querySelectorAll('.main-content > div[id^="tab-"]').forEach(el => el.classList.add('d-none'));
    document.getElementById(id)?.classList.remove('d-none');
    const loaders = { 'tab-types': loadTypes, 'tab-staff': loadStaff };
    if (loaders[id]) loaders[id]();
}

async function loadTypes() {
    try {
        const types = await get('/admin/membership-types');
        document.getElementById('types-table').innerHTML = types.map(t => `
            <tr>
                <td>${t.name}</td>
                <td>${t.durationDays}</td>
                <td>${t.price} ₽</td>
                <td><span class="badge ${t.active ? 'bg-success' : 'bg-secondary'}">${t.active ? 'Активен' : 'Неактивен'}</span></td>
                <td>
                    <button class="btn btn-sm btn-outline-primary" onclick="editType(${t.id},'${t.name.replace(/'/g,"\\'")}',${t.durationDays},${t.price},${t.active})">Изменить</button>
                    ${t.active ? `<button class="btn btn-sm btn-outline-danger ms-1" onclick="deleteType(${t.id})">Деакт.</button>` : ''}
                </td>
            </tr>`).join('');
    } catch (e) { showErr('alert-err', e.message); }
}

function showTypeForm() {
    document.getElementById('type-form-card').classList.remove('d-none');
    document.getElementById('type-form-title').textContent = 'Новый тип';
    document.getElementById('type-id').value = '';
    document.getElementById('type-form').reset();
}
function hideTypeForm() { document.getElementById('type-form-card').classList.add('d-none'); }

function editType(id, name, days, price, active) {
    document.getElementById('type-form-card').classList.remove('d-none');
    document.getElementById('type-form-title').textContent = 'Редактировать';
    document.getElementById('type-id').value = id;
    document.getElementById('type-name').value = name;
    document.getElementById('type-days').value = days;
    document.getElementById('type-price').value = price;
}

document.getElementById('type-form')?.addEventListener('submit', async e => {
    e.preventDefault();
    const id = document.getElementById('type-id').value;
    const body = {
        name: document.getElementById('type-name').value,
        durationDays: Number(document.getElementById('type-days').value),
        price: Number(document.getElementById('type-price').value),
        active: true
    };
    try {
        if (id) await put('/admin/membership-types/' + id, body);
        else await post('/admin/membership-types', body);
        showOk('alert-ok', 'Сохранено');
        hideTypeForm();
        loadTypes();
    } catch (err) { showErr('alert-err', err.message); }
});

async function deleteType(id) {
    if (!confirm('Деактивировать тип?')) return;
    try {
        await del('/admin/membership-types/' + id);
        showOk('alert-ok', 'Деактивирован');
        loadTypes();
    } catch (e) { showErr('alert-err', e.message); }
}

function initReportDates() {
    const now = new Date();
    const firstDay = new Date(now.getFullYear(), now.getMonth(), 1).toISOString().split('T')[0];
    const today = now.toISOString().split('T')[0];
    document.getElementById('rep-from').value = firstDay;
    document.getElementById('rep-to').value = today;
}

async function loadReport() {
    const from = document.getElementById('rep-from').value;
    const to = document.getElementById('rep-to').value;
    try {
        const report = await get(`/admin/reports/revenue?from=${from}&to=${to}`);
        document.getElementById('report-cards').innerHTML = `
            <div class="col-md-4">
                <div class="card card-stat revenue p-3">
                    <small class="text-muted">Выручка</small>
                    <h4>${report.totalRevenue.toLocaleString('ru-RU')} ₽</h4>
                </div>
            </div>
            <div class="col-md-4">
                <div class="card card-stat members p-3">
                    <small class="text-muted">Продано абонементов</small>
                    <h4>${report.totalMembershipsSold}</h4>
                </div>
            </div>
            <div class="col-md-4">
                <div class="card card-stat trainings p-3">
                    <small class="text-muted">Тренировок (всего)</small>
                    <h4>${report.totalTrainingsCount}</h4>
                </div>
            </div>`;
        const typeEntries = Object.entries(report.membershipsByType || {});
        if (typeEntries.length) {
            document.getElementById('report-types-table').innerHTML = typeEntries.map(([k, v]) =>
                `<tr><td>${k}</td><td>${v}</td></tr>`).join('');
            document.getElementById('report-table-container').classList.remove('d-none');
        }
    } catch (e) { showErr('alert-err', e.message); }
}

async function loadStaff() {
    try {
        const staff = await get('/admin/users');
        const roleMap = { ADMIN: 'Администратор', RECEPTION: 'Ресепшен', TRAINER: 'Тренер' };
        document.getElementById('staff-table').innerHTML = staff.map(s => `
            <tr>
                <td>${s.firstName} ${s.lastName}</td>
                <td>${s.email}</td>
                <td><span class="badge bg-secondary">${roleMap[s.role] || s.role}</span></td>
                <td>${s.phone || '—'}</td>
            </tr>`).join('');
    } catch (e) { showErr('alert-err', e.message); }
}

function toggleTrainerFields() {
    const role = document.getElementById('st-role').value;
    document.getElementById('trainer-fields').classList.toggle('d-none', role !== 'TRAINER');
}

document.getElementById('staff-form')?.addEventListener('submit', async e => {
    e.preventDefault();
    try {
        await post('/admin/users', {
            firstName: document.getElementById('st-first').value,
            lastName: document.getElementById('st-last').value,
            email: document.getElementById('st-email').value,
            password: document.getElementById('st-password').value,
            phone: document.getElementById('st-phone').value,
            role: document.getElementById('st-role').value,
            specialization: document.getElementById('st-spec').value,
            bio: document.getElementById('st-bio').value
        });
        showOk('alert-ok', 'Сотрудник создан');
        e.target.reset();
    } catch (e2) { showErr('alert-err', e2.message); }
});

loadTypes();
initReportDates();
