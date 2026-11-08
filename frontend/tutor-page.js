const me = App.requireRole('TUTOR');
if (me) {
  const $ = id => document.getElementById(id);
  const esc = App.escapeHtml;
  const days = ['Понедельник', 'Вторник', 'Среда', 'Четверг', 'Пятница', 'Суббота', 'Воскресенье'];
  const statusNames = { PENDING: 'Ожидает ответа', CONFIRMED: 'Подтверждено', COMPLETED: 'Проведено', CANCELLED: 'Отменено', REJECTED: 'Отклонено' };
  let offers = [];
  let allSubjects = [];
  let scheduleItems = [];
  let tutorProfile = null;

  function renderBooking(item, actions = false) {
    const student = item.student || {};
    const buttons = [];
    if (actions && item.status === 'PENDING') {
      buttons.push(`<button type="button" data-accept="${item.id}">Принять</button>`);
      buttons.push(`<button type="button" data-decline="${item.id}">Отклонить</button>`);
    }
    if (item.status === 'CONFIRMED' || item.status === 'COMPLETED') {
      buttons.push(`<button type="button" data-homework="${item.id}">Домашнее задание</button>`);
    }
    if (item.status === 'COMPLETED' && item.studentRating == null) {
      buttons.push(`<button type="button" data-rate="${item.id}">Оценить ученика</button>`);
    }
    return `<div class="item">
      <b>${esc(student.fullName || 'Ученик')}</b> — ${esc(item.subject?.name || '—')}<br>
      ${App.formatDateTime(item.startTime)} · ${Number(item.durationMinutes)} мин · ${Number(item.price).toLocaleString('ru-RU')} ₽<br>
      Телефон: ${esc(student.phone || 'не указан')} · Статус: ${statusNames[item.status] || esc(item.status)}
      ${item.homework ? `<p>Домашнее задание: ${esc(item.homework)}</p>` : ''}
      ${buttons.length ? `<p>${buttons.join(' ')}</p>` : ''}
    </div>`;
  }

  async function loadProfile() {
    const profile = await App.request('/tutor/profile');
    tutorProfile = profile;
    $('pageTitle').textContent = `Кабинет репетитора: ${me.fullName}`;
    $('bio').value = profile.bio || '';
    $('experienceYears').value = profile.experienceYears ?? 0;
  }

  async function loadHome() {
    const from = App.localDate();
    const to = App.datePlusDays(30);
    const [today, schedule] = await Promise.all([
      App.request('/tutor/lessons/today'),
      App.request(`/tutor/schedule?from=${from}&to=${to}`)
    ]);
    scheduleItems = schedule;
    $('todayList').innerHTML = today.map(item => renderBooking(item)).join('') || '<p>Сегодня занятий нет.</p>';
    $('scheduleList').innerHTML = schedule.map(item => renderBooking(item)).join('') || '<p>На выбранный период занятий нет.</p>';
  }

  async function loadRequestsAndStudents() {
    const [requests, students] = await Promise.all([
      App.request('/tutor/requests'), App.request('/tutor/students')
    ]);
    $('requestsList').innerHTML = requests.map(item => renderBooking(item, true)).join('') || '<p>Новых заявок нет.</p>';
    $('studentsList').innerHTML = students.map(student => `
      <div class="item"><b>${esc(student.fullName)}</b><br>
        Телефон: ${esc(student.phone || 'не указан')} · E-mail: ${esc(student.email || 'не указан')} · Рейтинг: ${Number(student.rating || 0).toFixed(1)} (${student.ratingCount || 0})
      </div>`).join('') || '<p>Пока нет учеников с активными занятиями.</p>';
  }

  async function loadServices() {
    [offers, allSubjects] = await Promise.all([App.request('/tutor/subjects'), App.request('/subjects')]);
    $('servicesBody').innerHTML = offers.map(offer => `
      <tr>
        <td>${esc(offer.subjectName)}</td>
        <td><input aria-label="Цена за час" type="number" min="0" step="50" value="${Number(offer.pricePerHour)}" data-price="${offer.subjectId}"></td>
        <td><button type="button" data-save-offer="${offer.subjectId}">Сохранить</button> <button type="button" data-delete-offer="${offer.subjectId}">Удалить</button></td>
      </tr>`).join('') || '<tr><td colspan="3">Предметы ещё не добавлены</td></tr>';
    const used = new Set(offers.map(o => o.subjectId));
    $('newSubject').innerHTML = allSubjects.filter(s => s.active && !used.has(s.id))
      .map(s => `<option value="${s.id}">${esc(s.name)}</option>`).join('') || '<option value="">Нет новых предметов</option>';
  }

  async function loadWorkingHours() {
    const hours = await App.request('/tutor/working-hours');
    $('workingHoursPanel').innerHTML = days.map((day, index) => {
      const dayNumber = index + 1;
      const windows = hours.filter(item => item.dayOfWeek === dayNumber);
      return `<div class="item" data-day-block="${dayNumber}">
        <b>${day}</b>
        <div data-windows="${dayNumber}">${windows.map((value, i) => `
          <div class="row" style="display:flex;gap:10px;align-items:center;flex-wrap:wrap;margin:8px 0">
            <label>С <input type="time" data-start="${dayNumber}" value="${(value.startTime || '09:00').slice(0, 5)}"></label>
            <label>До <input type="time" data-end="${dayNumber}" value="${(value.endTime || '18:00').slice(0, 5)}"></label>
            <button type="button" data-remove-window="${dayNumber}">Убрать интервал</button>
          </div>`).join('')}</div>
        <button type="button" data-add-window="${dayNumber}">Добавить интервал</button>
      </div>`;
    }).join('');
  }

  async function refreshAll() {
    try { await Promise.all([loadProfile(), loadHome(), loadRequestsAndStudents(), loadServices(), loadWorkingHours()]); }
    catch (error) { App.showError(error); }
  }

  $('profileForm').addEventListener('submit', async event => {
    event.preventDefault();
    try {
      await App.request('/tutor/profile', App.json('PUT', {
        bio: $('bio').value.trim(), photoUrl: tutorProfile?.photoUrl || null, experienceYears: Number($('experienceYears').value)
      }));
      App.toast('Профиль сохранён');
      await loadProfile();
    } catch (error) { App.showError(error); }
  });

  $('servicesBody').addEventListener('click', async event => {
    const save = event.target.closest('[data-save-offer]');
    const remove = event.target.closest('[data-delete-offer]');
    try {
      if (save) {
        const id = save.dataset.saveOffer;
        const pricePerHour = Number($(`price-${id}`)?.value ?? document.querySelector(`[data-price="${id}"]`).value);
        if (!Number.isFinite(pricePerHour) || pricePerHour < 0) return App.toast('Укажите корректную цену');
        await App.request(`/tutor/subjects/${id}`, App.json('PUT', { pricePerHour }));
        App.toast('Цена обновлена');
        await loadServices();
      }
      if (remove) {
        if (!confirm('Убрать этот предмет из профиля?')) return;
        await App.request(`/tutor/subjects/${remove.dataset.deleteOffer}`, { method: 'DELETE' });
        App.toast('Предмет удалён из профиля');
        await loadServices();
      }
    } catch (error) { App.showError(error); }
  });

  $('addService').addEventListener('click', async () => {
    const subjectId = Number($('newSubject').value);
    const pricePerHour = Number($('newPrice').value);
    if (!subjectId || !Number.isFinite(pricePerHour) || pricePerHour < 0) return App.toast('Выберите предмет и укажите цену');
    try {
      await App.request(`/tutor/subjects/${subjectId}`, App.json('PUT', { pricePerHour }));
      $('newPrice').value = '';
      App.toast('Предмет добавлен');
      await loadServices();
    } catch (error) { App.showError(error); }
  });

  $('workingHoursPanel').addEventListener('click', event => {
    const add = event.target.closest('[data-add-window]');
    const remove = event.target.closest('[data-remove-window]');
    if (add) {
      const day = Number(add.dataset.addWindow);
      const container = $('workingHoursPanel').querySelector(`[data-windows="${day}"]`);
      const row = document.createElement('div');
      row.className = 'row';
      row.style.cssText = 'display:flex;gap:10px;align-items:center;flex-wrap:wrap;margin:8px 0';
      row.innerHTML = `<label>С <input type="time" data-start="${day}" value="09:00"></label>
        <label>До <input type="time" data-end="${day}" value="18:00"></label>
        <button type="button" data-remove-window="${day}">Убрать интервал</button>`;
      container.appendChild(row);
    }
    if (remove) remove.closest('.row').remove();
  });

  $('saveWorkingHours').addEventListener('click', async () => {
    const items = [];
    for (let day = 1; day <= 7; day++) {
      const starts = [...$('workingHoursPanel').querySelectorAll(`[data-start="${day}"]`)];
      const ends = [...$('workingHoursPanel').querySelectorAll(`[data-end="${day}"]`)];
      const dayItems = [];
      for (let i = 0; i < starts.length; i++) {
        const startTime = starts[i].value;
        const endTime = ends[i]?.value;
        if (!startTime || !endTime || startTime >= endTime) return App.toast(`Проверьте время: ${days[day - 1]}`);
        dayItems.push({ dayOfWeek: day, startTime, endTime });
      }
      dayItems.sort((a, b) => a.startTime.localeCompare(b.startTime));
      for (let i = 1; i < dayItems.length; i++) {
        if (dayItems[i].startTime < dayItems[i - 1].endTime) return App.toast(`Интервалы пересекаются: ${days[day - 1]}`);
      }
      items.push(...dayItems);
    }
    try {
      await App.request('/tutor/working-hours', App.json('PUT', { items }));
      App.toast('Рабочий график сохранён');
      await loadWorkingHours();
    } catch (error) { App.showError(error); }
  });

  async function handleBookingAction(event) {
    const button = event.target.closest('[data-accept], [data-decline], [data-homework], [data-rate]');
    if (!button) return;
    const id = button.dataset.accept || button.dataset.decline || button.dataset.homework || button.dataset.rate;
    try {
      if (button.dataset.accept) await App.request(`/tutor/bookings/${id}/accept`, { method: 'POST' });
      if (button.dataset.decline) {
        if (!confirm('Отклонить заявку / отменить занятие?')) return;
        await App.request(`/tutor/bookings/${id}/decline`, { method: 'POST' });
      }
      if (button.dataset.homework) {
        const homework = prompt('Введите домашнее задание:');
        if (homework === null) return;
        if (!homework.trim()) return App.toast('Текст задания не должен быть пустым');
        await App.request(`/tutor/bookings/${id}/homework`, App.json('PUT', { homework: homework.trim() }));
      }
      if (button.dataset.rate) {
        const rating = Number(prompt('Оценка ученика от 1 до 5:'));
        if (!Number.isInteger(rating) || rating < 1 || rating > 5) return App.toast('Введите целое число от 1 до 5');
        await App.request(`/tutor/bookings/${id}/rate-student`, App.json('POST', { rating }));
      }
      App.toast('Изменения сохранены');
      await Promise.all([loadHome(), loadRequestsAndStudents()]);
    } catch (error) { App.showError(error); }
  }
  $('todayList').addEventListener('click', handleBookingAction);
  $('scheduleList').addEventListener('click', handleBookingAction);
  $('requestsList').addEventListener('click', handleBookingAction);

  document.querySelectorAll('.tabs button').forEach(button => button.addEventListener('click', () => {
    document.querySelectorAll('.tabs button').forEach(item => item.classList.toggle('active', item === button));
    ['home', 'requests', 'services', 'hours'].forEach(key => $(`tab-${key}`).classList.toggle('hidden', button.dataset.tab !== key));
  }));
  App.wireLogout();
  refreshAll();
}
