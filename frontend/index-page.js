const $ = id => document.getElementById(id);
const esc = App.escapeHtml;
let tutors = [];
let subjects = [];
let currentTutor = null;
let currentStep = 1;
let selected = { subjectId: null, duration: 60, date: '', slot: null, intro: false };

function roleLink() {
  const user = App.getUser();
  const button = $('loginBtn');
  if (user && App.getToken()) {
    button.textContent = 'Личный кабинет';
    button.onclick = () => location.href = App.pageForRole(user.role);
  } else {
    button.textContent = 'Войти';
    button.onclick = () => $('loginModal').classList.add('active');
  }
}

function initials(name) {
  return String(name || '?').split(/\s+/).map(word => word[0]).slice(0, 2).join('').toUpperCase();
}

function renderCards(list) {
  $('cards').innerHTML = list.map(t => {
    const offers = (t.offers || []).map(o => esc(o.subjectName)).join(', ');
    const photo = t.photoUrl ? `<img src="${esc(t.photoUrl)}" alt="${esc(t.fullName)}">` : initials(t.fullName);
    const price = t.minPricePerHour == null ? 'Цена не указана' : `${Number(t.minPricePerHour).toLocaleString('ru-RU')} ₽/час`;
    return `<article class="card">
      <div class="avatar">${photo}</div>
      <div class="card-body">
        <b>${esc(t.fullName)}</b> — ${offers || 'Предметы не указаны'}<br>
        ${esc(t.bio || 'Описание пока не добавлено')}<br>
        ★ ${Number(t.rating || 0).toFixed(1)} (${t.ratingCount || 0}) · опыт ${t.experienceYears || 0} лет · ${price}<br>
        <button type="button" data-book-tutor="${t.id}">Выбрать время</button>
      </div>
    </article>`;
  }).join('') || '<p>По заданным условиям репетиторы не найдены.</p>';
  $('count').textContent = `(${list.length})`;
}

async function loadSubjects() {
  subjects = await App.request('/subjects');
  $('fSubject').innerHTML = '<option value="">любой</option>' + subjects.map(s => `<option value="${s.id}">${esc(s.name)}</option>`).join('');
}

async function loadTutors() {
  const params = new URLSearchParams();
  if ($('fSubject').value) params.set('subjectId', $('fSubject').value);
  if ($('fPrice').value) params.set('maxPrice', $('fPrice').value);
  if ($('fRating').value) params.set('minRating', $('fRating').value);
  if ($('fExp').value) params.set('minExperience', $('fExp').value);
  params.set('sort', 'popular');
  $('cards').innerHTML = '<p>Загружаем каталог…</p>';
  tutors = await App.request(`/tutors?${params.toString()}`);
  renderCards(tutors);
}

function showStep(step) {
  currentStep = step;
  $('bStep1').classList.toggle('hidden', step !== 1);
  $('bStep2').classList.toggle('hidden', step !== 2);
  $('bStep3').classList.toggle('hidden', step !== 3);
  $('bBack').classList.toggle('hidden', step === 1);
  $('bNext').textContent = step === 3 ? 'Подтвердить запись' : 'Далее';
}

function selectedOffer() {
  return currentTutor?.offers?.find(o => String(o.subjectId) === String($('bSubject').value));
}

function updatePrice() {
  const intro = $('bIntro').checked;
  $('bDuration').disabled = intro;
  if (intro) $('bDuration').value = '30';
  selected.duration = Number($('bDuration').value);
  selected.intro = intro;
  const offer = selectedOffer();
  const price = intro ? 0 : (Number(offer?.pricePerHour || 0) * selected.duration / 60);
  $('bPriceLine').textContent = `${Math.round(price).toLocaleString('ru-RU')} ₽${intro ? ' (вводное занятие)' : ''}`;
}

async function openBooking(tutorId) {
  const user = App.getUser();
  if (!App.getToken() || !user) {
    $('loginModal').classList.add('active');
    App.toast('Для записи необходимо войти как ученик');
    return;
  }
  if (user.role !== 'STUDENT') {
    App.toast('Запись на занятия доступна из аккаунта ученика');
    return;
  }
  currentTutor = tutors.find(t => t.id === tutorId) || await App.request(`/tutors/${tutorId}`);
  if (!currentTutor?.offers?.length) return App.toast('У этого репетитора пока нет доступных предметов');
  selected = { subjectId: currentTutor.offers[0].subjectId, duration: 60, date: '', slot: null, intro: false };
  $('bTitle').textContent = `Запись к ${currentTutor.fullName}`;
  $('bSubject').innerHTML = currentTutor.offers.map(o => `<option value="${o.subjectId}">${esc(o.subjectName)}</option>`).join('');
  $('bDuration').value = '60';
  $('bIntro').checked = false;
  updatePrice();
  $('bSlots').innerHTML = '<p>Выберите дату.</p>';
  renderDates();
  showStep(1);
  $('bookingModal').classList.add('active');
}

function renderDates() {
  const dates = Array.from({ length: 10 }, (_, i) => App.datePlusDays(i + 1));
  $('bDates').innerHTML = dates.map((date, i) => {
    const label = new Intl.DateTimeFormat('ru-RU', { timeZone: 'Europe/Moscow', day: 'numeric', month: 'short', weekday: 'short' })
      .format(new Date(`${date}T12:00:00+03:00`));
    return `<button type="button" class="date-btn" data-date="${date}">${label}</button>`;
  }).join('');
  $('bDates').querySelectorAll('[data-date]').forEach(button => button.addEventListener('click', async () => {
    $('bDates').querySelectorAll('button').forEach(el => el.classList.toggle('active', el === button));
    selected.date = button.dataset.date;
    selected.slot = null;
    await loadSlots();
  }));
  $('bDates').querySelector('button')?.click();
}

async function loadSlots() {
  if (!currentTutor || !selected.date) return;
  updatePrice();
  const subjectId = $('bSubject').value;
  const duration = selected.intro ? 30 : Number($('bDuration').value);
  $('bSlots').innerHTML = '<p>Проверяем свободное время…</p>';
  try {
    const slots = await App.request(`/tutors/${currentTutor.id}/slots?subjectId=${subjectId}&durationMinutes=${duration}&date=${selected.date}`);
    if (!slots.length) {
      $('bSlots').innerHTML = '<p>На эту дату свободного времени нет. Выберите другой день.</p>';
      return;
    }
    $('bSlots').innerHTML = slots.map((slot, i) => {
      const time = new Intl.DateTimeFormat('ru-RU', { timeZone: 'Europe/Moscow', hour: '2-digit', minute: '2-digit' }).format(new Date(slot.start));
      return `<button type="button" data-slot-index="${i}">${time}</button>`;
    }).join('');
    $('bSlots').querySelectorAll('[data-slot-index]').forEach(button => button.addEventListener('click', () => {
      $('bSlots').querySelectorAll('button').forEach(el => el.classList.toggle('active', el === button));
      selected.slot = slots[Number(button.dataset.slotIndex)];
    }));
    selected.slot = null;
    window.__availableSlots = slots;
  } catch (error) {
    $('bSlots').innerHTML = `<p>${esc(error.message)}</p>`;
  }
}

function fillSummary() {
  const offer = selectedOffer();
  const price = selected.intro ? 0 : Number(offer.pricePerHour) * selected.duration / 60;
  $('bSummary').innerHTML = `
    <li>Репетитор: ${esc(currentTutor.fullName)}</li>
    <li>Предмет: ${esc(offer.subjectName)}</li>
    <li>Дата и время: ${App.formatDateTime(selected.slot.start)}</li>
    <li>Длительность: ${selected.intro ? 30 : selected.duration} мин</li>
    <li>Стоимость: ${Math.round(price).toLocaleString('ru-RU')} ₽</li>`;
}

$('searchBtn').addEventListener('click', () => loadTutors().catch(App.showError));
['fSubject', 'fPrice', 'fRating', 'fExp'].forEach(id => $(id).addEventListener('change', () => loadTutors().catch(App.showError)));
$('cards').addEventListener('click', event => {
  const button = event.target.closest('[data-book-tutor]');
  if (button) openBooking(Number(button.dataset.bookTutor)).catch(App.showError);
});
$('bSubject').addEventListener('change', () => { updatePrice(); if (selected.date) loadSlots(); });
$('bDuration').addEventListener('change', () => { updatePrice(); if (selected.date) loadSlots(); });
$('bIntro').addEventListener('change', () => { updatePrice(); if (selected.date) loadSlots(); });
$('bBack').addEventListener('click', () => showStep(Math.max(1, currentStep - 1)));
$('bNext').addEventListener('click', async () => {
  try {
    if (currentStep === 1) {
      selected.subjectId = Number($('bSubject').value);
      selected.duration = selected.intro ? 30 : Number($('bDuration').value);
      showStep(2);
      if (!selected.date) renderDates();
      return;
    }
    if (currentStep === 2) {
      if (!selected.slot) return App.toast('Сначала выберите свободное время');
      fillSummary();
      showStep(3);
      return;
    }
    const result = await App.request('/bookings', App.json('POST', {
      tutorId: currentTutor.id,
      subjectId: Number($('bSubject').value),
      startTime: selected.slot.start,
      durationMinutes: selected.intro ? 30 : selected.duration,
      intro: selected.intro
    }));
    $('bookingModal').classList.remove('active');
    App.toast(`Заявка №${result.id} отправлена. Ожидает подтверждения репетитором.`);
  } catch (error) { App.showError(error); }
});

$('loginForm').addEventListener('submit', async event => {
  event.preventDefault();
  try {
    const payload = await App.request('/auth/login', App.json('POST', {
      email: $('loginUser').value.trim(), password: $('loginPass').value
    }));
    App.saveAuth(payload);
    location.href = App.pageForRole(payload.user.role);
  } catch (error) { App.showError(error); }
});

document.querySelectorAll('[data-close]').forEach(button => button.addEventListener('click', () => {
  $(button.dataset.close).classList.remove('active');
}));
App.wireLogout();
roleLink();
if (new URLSearchParams(location.search).get('login') === '1') $('loginModal').classList.add('active');
Promise.all([loadSubjects(), loadTutors()]).catch(App.showError);
