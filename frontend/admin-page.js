const me = App.requireRole('ADMIN');
if (me) {
  const $ = id => document.getElementById(id);
  const esc = App.escapeHtml;
  const roleNames = { STUDENT: 'Ученик', TUTOR: 'Репетитор', ADMIN: 'Администратор' };
  const statusNames = { PENDING: 'Ожидает подтверждения', CONFIRMED: 'Подтверждено', COMPLETED: 'Проведено', CANCELLED: 'Отменено', REJECTED: 'Отклонено' };
  let users = [];
  let tutors = [];

  async function loadStats() {
    const stats = await App.request('/admin/stats');
    const usersByRole = stats.usersByRole || {};
    const bookings = stats.bookingsByStatus || {};
    $('statsPanel').innerHTML = `
      <div class="card"><div class="card-body">
        <b>Пользователи</b>: ${Object.values(usersByRole).reduce((a, b) => a + b, 0)}
        (ученики: ${usersByRole.STUDENT || 0}, репетиторы: ${usersByRole.TUTOR || 0}, администраторы: ${usersByRole.ADMIN || 0})<br>
        <b>Записи</b>: ${Object.values(bookings).reduce((a, b) => a + b, 0)}
        (ожидают: ${bookings.PENDING || 0}, подтверждены: ${bookings.CONFIRMED || 0}, проведены: ${bookings.COMPLETED || 0})<br>
        <b>Новые пользователи за 7 дней</b>: ${stats.newUsersLast7Days}<br>
        <b>Предстоящие занятия за 7 дней</b>: ${stats.upcomingBookingsNext7Days}<br>
        <b>Доля отмен</b>: ${Number(stats.cancellationRatePercent).toFixed(1)}%
      </div></div>
      <h3>Популярные репетиторы</h3>
      <ol>${(stats.topTutors || []).map(t => `<li>${esc(t.fullName)} — проведено занятий: ${t.completedLessons}, рейтинг: ${Number(t.rating).toFixed(1)}</li>`).join('') || '<li>Пока нет данных</li>'}</ol>
      <h3>Популярные предметы</h3>
      <ol>${(stats.topSubjects || []).map(s => `<li>${esc(s.name)} — записей: ${s.bookings}</li>`).join('') || '<li>Пока нет данных</li>'}</ol>`;
  }

  async function loadUsers(query = '') {
    const suffix = query ? `?q=${encodeURIComponent(query)}` : '';
    users = await App.request(`/admin/users${suffix}`);
    $('usersBody').innerHTML = users.map(user => `
      <tr>
        <td>${user.id}</td><td>${esc(user.fullName)}</td><td>${esc(user.email)}</td>
        <td>${roleNames[user.role] || esc(user.role)}</td>
        <td>${user.enabled ? 'Активен' : 'Заблокирован'}</td>
        <td>${user.id === me.id ? 'Текущий аккаунт' : `<button type="button" data-toggle-user="${user.id}" data-enabled="${user.enabled}">${user.enabled ? 'Заблокировать' : 'Разблокировать'}</button>`}</td>
      </tr>`).join('') || '<tr><td colspan="6">Пользователи не найдены</td></tr>';
    const available = users.filter(u => u.role === 'STUDENT' && !tutors.some(t => t.userId === u.id));
    $('tutorUser').innerHTML = available.map(u => `<option value="${u.id}">${esc(u.fullName)} — ${esc(u.email)}</option>`).join('') || '<option value="">Нет подходящих пользователей</option>';
  }

  async function loadTutors() {
    tutors = await App.request('/admin/tutors');
    $('tutorsBody').innerHTML = tutors.map(tutor => `
      <tr>
        <td>${esc(tutor.fullName)}</td>
        <td>${(tutor.offers || []).map(o => esc(o.subjectName)).join(', ') || 'Предметы не добавлены'}</td>
        <td>${tutor.experienceYears}</td>
        <td>${Number(tutor.rating || 0).toFixed(1)} (${tutor.ratingCount || 0})</td>
        <td>${tutor.active ? 'Активен' : 'Отключён'}</td>
        <td><button type="button" data-toggle-tutor="${tutor.id}" data-active="${tutor.active}">${tutor.active ? 'Отключить' : 'Активировать'}</button></td>
      </tr>`).join('') || '<tr><td colspan="6">Профилей репетиторов пока нет</td></tr>';
  }

  async function loadSubjects() {
    const subjects = await App.request('/admin/subjects');
    $('subjectsBody').innerHTML = subjects.map(subject => `
      <tr><td>${esc(subject.name)}</td><td>${subject.active ? 'Активен' : 'Отключён'}</td>
      <td>${subject.active ? `<button type="button" data-disable-subject="${subject.id}">Отключить</button>` : '—'}</td></tr>`).join('') || '<tr><td colspan="3">Предметов нет</td></tr>';
  }

  async function loadBookings() {
    const from = App.datePlusDays(-30);
    const to = App.datePlusDays(30);
    const bookings = await App.request(`/admin/schedule?from=${from}&to=${to}`);
    $('bookingsBody').innerHTML = bookings.map(item => `
      <tr>
        <td>${App.formatDateTime(item.startTime)}</td>
        <td>${esc(item.student?.fullName || '—')}</td>
        <td>${esc(item.tutor?.fullName || '—')}</td>
        <td>${esc(item.subject?.name || '—')}</td>
        <td>${statusNames[item.status] || esc(item.status)}</td>
        <td>${['PENDING', 'CONFIRMED'].includes(item.status) ? `<button type="button" data-cancel-booking="${item.id}">Отменить</button>` : '—'}</td>
      </tr>`).join('') || '<tr><td colspan="6">Записей за выбранный период нет</td></tr>';
  }

  $('searchUsers').addEventListener('click', () => loadUsers($('userSearch').value.trim()).catch(App.showError));
  $('userSearch').addEventListener('keydown', event => { if (event.key === 'Enter') loadUsers($('userSearch').value.trim()).catch(App.showError); });

  $('usersBody').addEventListener('click', async event => {
    const button = event.target.closest('[data-toggle-user]');
    if (!button) return;
    try {
      await App.request(`/admin/users/${button.dataset.toggleUser}`, App.json('PATCH', { enabled: button.dataset.enabled !== 'true' }));
      App.toast('Статус пользователя изменён');
      await loadUsers($('userSearch').value.trim());
    } catch (error) { App.showError(error); }
  });

  $('createTutor').addEventListener('click', async () => {
    const userId = Number($('tutorUser').value);
    if (!userId) return App.toast('Нет пользователя для создания профиля');
    try {
      await App.request('/admin/tutors', App.json('POST', {
        userId,
        bio: $('tutorBio').value.trim() || null,
        photoUrl: null,
        experienceYears: Number($('tutorExperience').value || 0)
      }));
      App.toast('Профиль репетитора создан. Теперь добавьте ему предметы и цены.');
      $('tutorBio').value = '';
      $('tutorExperience').value = '0';
      await Promise.all([loadUsers(), loadTutors()]);
    } catch (error) { App.showError(error); }
  });

  $('tutorsBody').addEventListener('click', async event => {
    const button = event.target.closest('[data-toggle-tutor]');
    if (!button) return;
    const tutor = tutors.find(t => String(t.id) === button.dataset.toggleTutor);
    if (!tutor) return;
    try {
      await App.request(`/admin/tutors/${tutor.id}`, App.json('PUT', {
        bio: tutor.bio || null, photoUrl: tutor.photoUrl || null,
        experienceYears: tutor.experienceYears || 0, active: button.dataset.active !== 'true'
      }));
      App.toast('Статус профиля обновлён');
      await loadTutors();
    } catch (error) { App.showError(error); }
  });

  $('createSubject').addEventListener('click', async () => {
    const name = $('newSubjectName').value.trim();
    if (!name) return App.toast('Введите название предмета');
    try {
      await App.request('/admin/subjects', App.json('POST', { name, active: true }));
      $('newSubjectName').value = '';
      App.toast('Предмет добавлен');
      await loadSubjects();
    } catch (error) { App.showError(error); }
  });

  $('subjectsBody').addEventListener('click', async event => {
    const button = event.target.closest('[data-disable-subject]');
    if (!button || !confirm('Отключить предмет? Старые записи сохранятся.')) return;
    try {
      await App.request(`/admin/subjects/${button.dataset.disableSubject}`, { method: 'DELETE' });
      App.toast('Предмет отключён');
      await loadSubjects();
    } catch (error) { App.showError(error); }
  });

  $('bookingsBody').addEventListener('click', async event => {
    const button = event.target.closest('[data-cancel-booking]');
    if (!button || !confirm('Отменить это занятие?')) return;
    try {
      await App.request(`/admin/bookings/${button.dataset.cancelBooking}/cancel`, { method: 'POST' });
      App.toast('Запись отменена');
      await Promise.all([loadBookings(), loadStats()]);
    } catch (error) { App.showError(error); }
  });

  App.wireLogout();
  Promise.all([loadStats(), loadTutors(), loadSubjects(), loadBookings()])
    .then(() => loadUsers())
    .catch(App.showError);
}
