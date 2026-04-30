if (!checkAuth('TRAINER')) { /* redirected */ }
document.getElementById('sidebar-name').textContent = getUserName();

function showTab(id) {
    document.querySelectorAll('.main-content > div[id^="tab-"]').forEach(el => el.classList.add('d-none'));
    document.getElementById(id)?.classList.remove('d-none');
    const loaders = { 'tab-schedule': loadSchedule, 'tab-clients': loadClients, 'tab-programs': loadPrograms, 'tab-notifications': loadNotifications };
    if (loaders[id]) loaders[id]();
}

async function loadSchedule() {
    try {
        const bookings = await get('/trainer/bookings');
        const statusMap = {
            CONFIRMED: '<span class="badge bg-success">Подтверждена</span>',
            PENDING: '<span class="badge bg-warning text-dark">Ожидает</span>',
            CANCELLED_BY_CLIENT: '<span class="badge bg-secondary">Отменена клиентом</span>',
            CANCELLED_BY_TRAINER: '<span class="badge bg-danger">Отменена вами</span>',
            COMPLETED: '<span class="badge bg-primary">Завершена</span>'
        };
        document.getElementById('schedule-table').innerHTML = bookings.map(b => `
            <tr>
                <td>${b.clientName}<br><small class="text-muted">${b.clientEmail}</small></td>
                <td>${fmtDt(b.startDateTime)}</td>
                <td>${statusMap[b.status] || b.status}</td>
                <td>
                    ${b.status === 'PENDING' ? `<button class="btn btn-sm btn-success me-1" onclick="confirmBooking(${b.id})">✓ Подтвердить</button>` : ''}
                    ${(b.status === 'CONFIRMED' || b.status === 'PENDING') ?
            `<button class="btn btn-sm btn-outline-danger" onclick="cancelBooking(${b.id})">Отменить</button>` : ''}
                </td>
            </tr>`).join('') || '<tr><td colspan="4" class="text-center text-muted">Нет предстоящих тренировок</td></tr>';
    } catch (e) { showErr('alert-err', e.message); }
}

async function confirmBooking(id) {
    try {
        await put('/trainer/bookings/' + id + '/confirm', {});
        showOk('alert-ok', 'Подтверждено');
        loadSchedule();
    } catch (e) { showErr('alert-err', e.message); }
}

async function cancelBooking(id) {
    const reason = prompt('Укажите причину отмены:');
    if (reason === null) return;
    try {
        await put('/trainer/bookings/' + id + '/cancel', { reason });
        showOk('alert-ok', 'Тренировка отменена');
        loadSchedule();
    } catch (e) { showErr('alert-err', e.message); }
}

let myClients = [];

async function loadClients() {
    try {
        myClients = await get('/trainer/clients');
        document.getElementById('clients-table').innerHTML = myClients.map(c => `
            <tr>
                <td>${c.firstName} ${c.lastName}</td>
                <td>${c.email}</td>
                <td>
                    <button class="btn btn-sm btn-outline-primary" onclick="viewClientProgram(${c.id})">📋 Программа</button>
                </td>
            </tr>`).join('') || '<tr><td colspan="3" class="text-center text-muted">Нет клиентов</td></tr>';
    } catch (e) { showErr('alert-err', e.message); }
}

async function viewClientProgram(clientId) {
    showTab('tab-programs');
    try {
        const program = await get('/trainer/clients/' + clientId + '/workout-program');
        if (program && program.id) {
            openEditProgram(program);
        } else {
            openNewProgram(clientId);
        }
    } catch (e) {
        openNewProgram(clientId);
    }
}

async function loadPrograms() {
    try {
        const programs = await get('/trainer/programs');
        const el = document.getElementById('programs-list');
        if (!programs.length) { el.innerHTML = '<p class="text-muted">Нет созданных программ</p>'; return; }
        el.innerHTML = programs.map(p => `
            <div class="card mb-2">
                <div class="card-body d-flex justify-content-between align-items-center">
                    <div>
                        <strong>${p.name}</strong> <span class="text-muted ms-2">→ ${p.clientName}</span>
                        <br><small class="text-muted">${p.exercises?.length || 0} упражнений</small>
                    </div>
                    <button class="btn btn-sm btn-outline-primary" onclick="editProgram(${p.id},${p.clientId})">Редактировать</button>
                </div>
            </div>`).join('');
    } catch (e) { showErr('alert-err', e.message); }
}

async function openNewProgram(preselectedClientId) {
    if (!myClients.length) await loadClients();
    const editor = document.getElementById('program-editor');
    editor.classList.remove('d-none');
    document.getElementById('editor-title').textContent = 'Новая программа';
    document.getElementById('prog-id').value = '';
    document.getElementById('prog-name').value = '';
    document.getElementById('exercises-list').innerHTML = '';

    const sel = document.getElementById('prog-client-select');
    sel.innerHTML = myClients.map(c => `<option value="${c.id}" ${c.id == preselectedClientId ? 'selected' : ''}>${c.firstName} ${c.lastName}</option>`).join('');
    addExerciseRow();
}

async function editProgram(programId, clientId) {
    try {
        const prog = await get('/trainer/clients/' + clientId + '/workout-program');
        openEditProgram(prog);
    } catch (e) { showErr('alert-err', e.message); }
}

function openEditProgram(prog) {
    if (!myClients.length) loadClients();
    const editor = document.getElementById('program-editor');
    editor.classList.remove('d-none');
    document.getElementById('editor-title').textContent = 'Редактировать программу';
    document.getElementById('prog-id').value = prog.id;
    document.getElementById('prog-name').value = prog.name;
    const sel = document.getElementById('prog-client-select');
    sel.innerHTML = `<option value="${prog.clientId}">${prog.clientName}</option>`;

    const list = document.getElementById('exercises-list');
    list.innerHTML = '';
    (prog.exercises || []).forEach(ex => addExerciseRow(ex));
}

function closeProgramEditor() {
    document.getElementById('program-editor').classList.add('d-none');
}

let exIdx = 0;
function addExerciseRow(ex) {
    const idx = exIdx++;
    const day = ex?.dayNumber || 1;
    const div = document.createElement('div');
    div.className = 'row g-2 mb-2 exercise-row';
    div.innerHTML = `
        <div class="col-3"><input class="form-control form-control-sm" placeholder="Напр. Жим лёжа" value="${ex?.exerciseName || ''}" data-field="exerciseName" required></div>
        <div class="col-1"><input class="form-control form-control-sm" type="number" placeholder="Подходы" value="${ex?.sets || 3}" min="1" data-field="sets"></div>
        <div class="col-1"><input class="form-control form-control-sm" placeholder="Напр. 10 или 8-12" value="${ex?.reps || '10'}" data-field="reps"></div>
        <div class="col-2"><input class="form-control form-control-sm" placeholder="Напр. 60 кг или без веса" value="${ex?.weight || ''}" data-field="weight"></div>
        <div class="col-1"><input class="form-control form-control-sm" type="number" placeholder="№ дня" value="${day}" min="1" data-field="dayNumber"></div>
        <div class="col-auto"><button class="btn btn-sm btn-outline-danger" onclick="this.closest('.exercise-row').remove()">✕</button></div>`;
    document.getElementById('exercises-list').appendChild(div);
}

async function saveProgram() {
    const name = document.getElementById('prog-name').value;
    const clientId = document.getElementById('prog-client-select').value;
    const progId = document.getElementById('prog-id').value;
    if (!name || !clientId) { showErr('alert-err', 'Заполните название и выберите клиента'); return; }

    const exercises = [];
    document.querySelectorAll('#exercises-list .exercise-row').forEach((row, idx) => {
        const get = f => row.querySelector(`[data-field="${f}"]`).value;
        exercises.push({
            exerciseName: get('exerciseName'), sets: Number(get('sets')),
            reps: get('reps'), weight: get('weight'),
            dayNumber: Number(get('dayNumber')) || 1, orderIndex: idx
        });
    });

    const body = { name, exercises };
    try {
        if (progId) {
            await put(`/trainer/clients/${clientId}/workout-program/${progId}`, body);
        } else {
            await post(`/trainer/clients/${clientId}/workout-program`, body);
        }
        showOk('alert-ok', 'Программа сохранена');
        closeProgramEditor();
        loadPrograms();
    } catch (e) { showErr('alert-err', e.message); }
}

async function loadNotifications() {
    try {
        const notifications = await get('/trainer/notifications');
        const el = document.getElementById('notifications-list');
        if (!notifications.length) { el.innerHTML = '<p class="text-muted">Нет новых уведомлений</p>'; return; }
        el.innerHTML = notifications.map(n => `
            <div class="alert alert-info py-2">
                <small class="text-muted">${fmtDt(n.createdAt)}</small><br>${n.message}
            </div>`).join('');
    } catch (e) { showErr('alert-err', e.message); }
}

async function markAllRead() {
    await post('/trainer/notifications/read', {});
    loadNotifications();
}

loadSchedule();
