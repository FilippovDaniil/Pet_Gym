if (!checkAuth('CLIENT')) { /* redirected */ }

document.getElementById('sidebar-name').textContent = getUserName();

let selectedTrainerId = null;

function showTab(id) {
    document.querySelectorAll('.main-content > div[id^="tab-"]').forEach(el => el.classList.add('d-none'));
    document.getElementById(id).classList.remove('d-none');
    document.querySelectorAll('.sidebar .nav-link').forEach(l => l.classList.remove('active'));
    const loaders = { 'tab-membership': loadMemberships, 'tab-trainers': loadTrainers,
        'tab-bookings': loadBookings, 'tab-program': loadProgram, 'tab-notifications': loadNotifications };
    if (loaders[id]) loaders[id]();
}

async function loadMemberships() {
    try {
        const [purchases, types] = await Promise.all([get('/client/memberships/active'), get('/client/memberships/types')]);
        const listEl = document.getElementById('membership-list');
        if (!purchases.length) {
            listEl.innerHTML = '<p class="text-muted">У вас нет абонементов</p>';
        } else {
            listEl.innerHTML = '<div class="row g-3">' + purchases.map(p => `
                <div class="col-md-4">
                    <div class="card ${p.active ? 'border-success' : 'border-secondary'}">
                        <div class="card-body">
                            <h6 class="card-title">${p.typeName}</h6>
                            <p class="mb-1 small">С: ${fmtDate(p.startDate)}</p>
                            <p class="mb-1 small">По: ${fmtDate(p.endDate)}</p>
                            <p class="mb-0 small">Оплачено: ${p.paidAmount} ₽</p>
                            <span class="badge ${p.active ? 'bg-success' : 'bg-secondary'} mt-2">${p.active ? 'Активен' : 'Истёк'}</span>
                        </div>
                    </div>
                </div>`).join('') + '</div>';
        }
        const typesEl = document.getElementById('types-list');
        typesEl.innerHTML = types.map(t => `
            <div class="col-md-4">
                <div class="card h-100">
                    <div class="card-body">
                        <h6>${t.name}</h6>
                        <p class="text-muted small">${t.durationDays} дней</p>
                        <p class="fw-bold fs-5">${t.price} ₽</p>
                        <button class="btn btn-primary btn-sm" onclick="buyMembership(${t.id})">Купить</button>
                    </div>
                </div>
            </div>`).join('');
    } catch (e) { showErr('alert-err', e.message); }
}

async function buyMembership(typeId) {
    try {
        await post('/client/memberships/buy/' + typeId, {});
        showOk('alert-ok', 'Абонемент успешно куплен!');
        loadMemberships();
    } catch (e) { showErr('alert-err', e.message); }
}

async function loadTrainers() {
    try {
        const trainers = await get('/client/trainers');
        document.getElementById('trainers-list').innerHTML = trainers.map(t => `
            <div class="col-md-4">
                <div class="card h-100">
                    <div class="card-body">
                        <h6>${t.firstName} ${t.lastName}</h6>
                        <p class="text-muted small">${t.specialization || ''}</p>
                        <p class="small">${t.bio || ''}</p>
                        <button class="btn btn-outline-primary btn-sm" onclick="selectTrainer(${t.id}, '${t.firstName} ${t.lastName}')">Забронировать</button>
                    </div>
                </div>
            </div>`).join('');
    } catch (e) { showErr('alert-err', e.message); }
}

function selectTrainer(id, name) {
    selectedTrainerId = id;
    document.getElementById('selected-trainer-name').textContent = name;
    const panel = document.getElementById('booking-panel');
    panel.classList.remove('d-none');
    // Локальная дата браузера (не UTC, чтобы не смещало на сутки в других часовых поясах)
    const now = new Date();
    const localDate = d => d.getFullYear() + '-' +
        String(d.getMonth() + 1).padStart(2, '0') + '-' +
        String(d.getDate()).padStart(2, '0');
    const maxDate = new Date(now); maxDate.setDate(now.getDate() + 30);
    const input = document.getElementById('slot-date');
    input.min = localDate(now);
    input.max = localDate(maxDate);
    input.value = localDate(now);
    document.getElementById('slots-container').innerHTML = '';
}

async function loadSlots() {
    const date = document.getElementById('slot-date').value;
    if (!date || !selectedTrainerId) return;
    try {
        const slots = await get(`/client/trainers/${selectedTrainerId}/slots?date=${date}`);
        const container = document.getElementById('slots-container');
        if (!slots.length) {
            container.innerHTML = '<p class="text-muted">Нет свободных слотов на эту дату</p>';
            return;
        }
        container.innerHTML = '<div>' + slots.map(s => {
            const time = new Date(s).toLocaleTimeString('ru-RU', { hour: '2-digit', minute: '2-digit' });
            return `<button class="btn btn-outline-success slot-btn" onclick="book('${s}')">${time}</button>`;
        }).join('') + '</div>';
    } catch (e) { showErr('alert-err', e.message); }
}

async function book(startDateTime) {
    if (!selectedTrainerId) return;
    try {
        await post('/client/bookings', { trainerId: selectedTrainerId, startDateTime });
        showOk('alert-ok', 'Тренировка забронирована!');
        document.getElementById('booking-panel').classList.add('d-none');
        loadSlots();
    } catch (e) { showErr('alert-err', e.message); }
}

async function loadBookings() {
    try {
        const bookings = await get('/client/bookings');
        const statusMap = { CONFIRMED: '<span class="badge bg-success">Подтверждена</span>',
            PENDING: '<span class="badge bg-warning text-dark">Ожидает</span>',
            CANCELLED_BY_CLIENT: '<span class="badge bg-secondary">Отменена вами</span>',
            CANCELLED_BY_TRAINER: '<span class="badge bg-danger">Отменена тренером</span>',
            COMPLETED: '<span class="badge bg-primary">Завершена</span>' };
        document.getElementById('bookings-table').innerHTML = bookings.map(b => `
            <tr>
                <td>${b.trainerName}</td>
                <td>${fmtDt(b.startDateTime)}</td>
                <td>${statusMap[b.status] || b.status}</td>
                <td>${(b.status === 'CONFIRMED' || b.status === 'PENDING') ?
            `<button class="btn btn-sm btn-outline-danger" onclick="cancelBooking(${b.id})">Отменить</button>` : ''}</td>
            </tr>`).join('');
    } catch (e) { showErr('alert-err', e.message); }
}

async function cancelBooking(id) {
    if (!confirm('Отменить бронирование?')) return;
    try {
        await del('/client/bookings/' + id);
        showOk('alert-ok', 'Бронирование отменено');
        loadBookings();
    } catch (e) { showErr('alert-err', e.message); }
}

async function loadProgram() {
    try {
        const program = await get('/client/workout-program');
        const el = document.getElementById('program-content');
        if (!program || !program.id) {
            el.innerHTML = `
                <div class="text-center py-5 text-muted">
                    <div style="font-size:3rem">📋</div>
                    <p class="mt-2">Программа тренировок ещё не назначена тренером</p>
                </div>`;
            return;
        }

        const days = {};
        (program.exercises || [])
            .sort((a, b) => (a.orderIndex ?? 0) - (b.orderIndex ?? 0))
            .forEach(ex => {
                if (!days[ex.dayNumber]) days[ex.dayNumber] = [];
                days[ex.dayNumber].push(ex);
            });

        const totalDays = Object.keys(days).length;
        const totalExercises = program.exercises?.length || 0;
        const totalSets = program.exercises?.reduce((sum, ex) => sum + (ex.sets || 0), 0) || 0;

        el.innerHTML = `
            <div class="mb-3">
                <h5 class="mb-1">${program.name}</h5>
                <span class="text-muted small">Тренер: ${program.trainerName}</span>
            </div>
            <div class="row g-2 mb-4">
                <div class="col-auto">
                    <div class="border rounded px-3 py-2 text-center">
                        <div class="fw-bold fs-5">${totalDays}</div>
                        <div class="text-muted small">дней</div>
                    </div>
                </div>
                <div class="col-auto">
                    <div class="border rounded px-3 py-2 text-center">
                        <div class="fw-bold fs-5">${totalExercises}</div>
                        <div class="text-muted small">упражнений</div>
                    </div>
                </div>
                <div class="col-auto">
                    <div class="border rounded px-3 py-2 text-center">
                        <div class="fw-bold fs-5">${totalSets}</div>
                        <div class="text-muted small">подходов</div>
                    </div>
                </div>
            </div>` +
            Object.entries(days)
                .sort(([a], [b]) => Number(a) - Number(b))
                .map(([day, exs]) => {
                    const daySets = exs.reduce((sum, ex) => sum + (ex.sets || 0), 0);
                    return `
                    <div class="card mb-3">
                        <div class="card-header d-flex justify-content-between align-items-center">
                            <span class="fw-bold">День ${day}</span>
                            <span class="text-muted small">${exs.length} упр. · ${daySets} подходов</span>
                        </div>
                        <div class="card-body p-0">
                            <table class="table table-sm mb-0">
                                <thead class="table-light">
                                    <tr>
                                        <th>Упражнение</th>
                                        <th class="text-center">Подходы</th>
                                        <th class="text-center">Повторения</th>
                                        <th class="text-center">Вес</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    ${exs.map((e, i) => `
                                    <tr>
                                        <td><span class="text-muted small me-1">${i + 1}.</span>${e.exerciseName}</td>
                                        <td class="text-center"><span class="badge bg-primary">${e.sets}</span></td>
                                        <td class="text-center"><span class="badge bg-info text-dark">× ${e.reps}</span></td>
                                        <td class="text-center">${e.weight ? `<span class="badge bg-secondary">${e.weight}</span>` : '<span class="text-muted">—</span>'}</td>
                                    </tr>`).join('')}
                                </tbody>
                            </table>
                        </div>
                    </div>`;
                }).join('');
    } catch (e) {
        if (e.message.includes('204') || e.message === '') {
            document.getElementById('program-content').innerHTML = '<p class="text-muted">Программа не назначена</p>';
        } else showErr('alert-err', e.message);
    }
}

async function loadNotifications() {
    try {
        const notifications = await get('/client/notifications');
        const el = document.getElementById('notifications-list');
        if (!notifications.length) { el.innerHTML = '<p class="text-muted">Нет новых уведомлений</p>'; return; }
        el.innerHTML = notifications.map(n => `
            <div class="alert alert-info py-2">
                <small class="text-muted">${fmtDt(n.createdAt)}</small><br>${n.message}
            </div>`).join('');
    } catch (e) { showErr('alert-err', e.message); }
}

async function markAllRead() {
    await post('/client/notifications/read', {});
    loadNotifications();
}

// Init
loadMemberships();
