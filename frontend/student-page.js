const me = App.requireRole('STUDENT');
if (me) {
  const $ = id => document.getElementById(id);
  const esc = App.escapeHtml;
  const statusNames = { PENDING: 'Ожидает подтверждения', CONFIRMED: 'Подтверждено', COMPLETED: 'Проведено', CANCELLED: 'Отменено', REJECTED: 'Отклонено' };

  function renderProfile(user) {
    $('profileInfo').innerHTML = `
      <li>ФИО: <b>${esc(user.fullName)}</b></li>
      <li>E-mail: ${esc(user.email)}</li>
      <li>Телефон: ${esc(user.phone || 'не указан')}</li>
      <li>Дата рождения: ${esc(App.formatDate(user.birthDate))}</li>`;
  }
  function bookingDate(booking) { return App.formatDateTime(booking.startTime); }
  function renderUpcoming(items) {
    $('upcomingBody').innerHTML = items.map(item => `
      <tr>
        <td>${bookingDate(item)}</td>
        <td>${esc(item.tutor?.fullName || '—')}</td>
        <td>${esc(item.subject?.name || '—')}</td>
        <td>${item.intro ? 'Бесплатно' : `${Number(item.price).toLocaleString('ru-RU')} ₽`}</td>
        <td>${statusNames[item.status] || esc(item.status)}</td>
        <td><button type="button" data-cancel="${item.id}">Отменить</button></td>
      </tr>`).join('') || '<tr><td colspan="6">Предстоящих занятий пока нет</td></tr>';
  }
  function renderPast(items) {
    $('pastBody').innerHTML = items.map(item => {
      const canRate = item.status === 'COMPLETED' && item.tutorRating == null;
      const rating = item.tutorRating == null ? (item.status === 'COMPLETED' ? 'Не оценено' : '—') : `${item.tutorRating}/5`;
      return `<tr>
        <td>${bookingDate(item)}</td>
        <td>${esc(item.tutor?.fullName || '—')}</td>
        <td>${esc(item.subject?.name || '—')}</td>
        <td>${statusNames[item.status] || esc(item.status)}</td>
        <td>${esc(item.homework || '—')}</td>
        <td>${canRate ? `<button type="button" data-rate="${item.id}">Оценить</button>` : rating}</td>
      </tr>`;
    }).join('') || '<tr><td colspan="6">История занятий пуста</td></tr>';
  }
  async function load() {
    try {
      const [profile, upcoming, past] = await Promise.all([
        App.request('/me'), App.request('/bookings/my/upcoming'), App.request('/bookings/my/past')
      ]);
      renderProfile(profile);
      renderUpcoming(upcoming);
      renderPast(past);
    } catch (error) { App.showError(error); }
  }
  $('upcomingBody').addEventListener('click', async event => {
    const button = event.target.closest('[data-cancel]');
    if (!button || !confirm('Отменить выбранное занятие?')) return;
    try {
      await App.request(`/bookings/${button.dataset.cancel}/cancel`, { method: 'POST' });
      App.toast('Запись отменена');
      await load();
    } catch (error) { App.showError(error); }
  });
  $('pastBody').addEventListener('click', async event => {
    const button = event.target.closest('[data-rate]');
    if (!button) return;
    const value = Number(prompt('Оцените занятие от 1 до 5:'));
    if (!Number.isInteger(value) || value < 1 || value > 5) return App.toast('Введите целое число от 1 до 5');
    try {
      await App.request(`/bookings/${button.dataset.rate}/rate`, App.json('POST', { rating: value }));
      App.toast('Спасибо за оценку!');
      await load();
    } catch (error) { App.showError(error); }
  });
  document.querySelectorAll('.tabs button').forEach(button => button.addEventListener('click', () => {
    document.querySelectorAll('.tabs button').forEach(item => item.classList.toggle('active', item === button));
    $('tab-upcoming').classList.toggle('hidden', button.dataset.tab !== 'upcoming');
    $('tab-past').classList.toggle('hidden', button.dataset.tab !== 'past');
  }));
  App.wireLogout();
  load();
}
