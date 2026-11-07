package ru.vsuet.team.tutoringbackend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.vsuet.team.tutoringbackend.booking.Booking;
import ru.vsuet.team.tutoringbackend.booking.repository.BookingRepository;
import ru.vsuet.team.tutoringbackend.booking.BookingStatus;
import ru.vsuet.team.tutoringbackend.catalog.entity.Subject;
import ru.vsuet.team.tutoringbackend.catalog.entity.Tutor;
import ru.vsuet.team.tutoringbackend.catalog.entity.TutorSubject;
import ru.vsuet.team.tutoringbackend.catalog.repository.SubjectRepository;
import ru.vsuet.team.tutoringbackend.catalog.repository.TutorRepository;
import ru.vsuet.team.tutoringbackend.catalog.repository.TutorSubjectRepository;
import ru.vsuet.team.tutoringbackend.schedule.entity.WorkingHours;
import ru.vsuet.team.tutoringbackend.schedule.repository.WorkingHoursRepository;
import ru.vsuet.team.tutoringbackend.user.Role;
import ru.vsuet.team.tutoringbackend.user.User;
import ru.vsuet.team.tutoringbackend.user.repository.UserRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.HashMap;
import java.util.Map;

@Component
@Profile("demo")
@RequiredArgsConstructor
public class DemoDataSeeder implements CommandLineRunner {
    private final UserRepository users;
    private final SubjectRepository subjects;
    private final TutorRepository tutors;
    private final TutorSubjectRepository offers;
    private final WorkingHoursRepository hours;
    private final BookingRepository bookings;
    private final PasswordEncoder encoder;
    private final AppProperties props;
    private final Clock clock;

    private final Map<String, BigDecimal> prices = new HashMap<>();   // "tutorId:subjectId" -> цена за час
    private int phoneSeq = 0;

    @Override
    @Transactional
    public void run(String... args) {
        if (users.count() > 0) return;     // не трогаем БД, восстановленную из дампа

        // ===================== пользователи =====================
        user("admin@demo.com", "Admin123!", "Администратор", Role.ADMIN, null);

        User t1u = user("tutor1@demo.com", "Tutor123!", "Анна Смирнова", Role.TUTOR, null);
        User t2u = user("tutor2@demo.com", "Tutor123!", "Дмитрий Орлов", Role.TUTOR, null);
        User t3u = user("tutor3@demo.com", "Tutor123!", "Елена Васильева", Role.TUTOR, null);
        User t4u = user("tutor4@demo.com", "Tutor123!", "Игорь Козлов", Role.TUTOR, null);
        User t5u = user("tutor5@demo.com", "Tutor123!", "Ольга Новикова", Role.TUTOR, null);

        User st1 = user("student@demo.com", "Student123!", "Иван Петров", Role.STUDENT, LocalDate.of(2009, 5, 14));
        User st2 = user("student2@demo.com", "Student123!", "Мария Сидорова", Role.STUDENT, LocalDate.of(2007, 9, 2));
        User st3 = user("student3@demo.com", "Student123!", "Артём Кузнецов", Role.STUDENT, LocalDate.of(2010, 2, 21));
        User st4 = user("student4@demo.com", "Student123!", "София Лебедева", Role.STUDENT, LocalDate.of(2008, 11, 30));

        // ===================== 10 предметов =====================
        Subject math = subject("Математика");
        Subject physics = subject("Физика");
        Subject chemistry = subject("Химия");
        Subject biology = subject("Биология");
        Subject informatics = subject("Информатика");
        Subject russian = subject("Русский язык");
        Subject literature = subject("Литература");
        Subject history = subject("История");
        Subject english = subject("Английский язык");
        Subject german = subject("Немецкий язык");

        // ===================== 5 репетиторов: предметы, цены, расписание =====================
        // tutor1 и tutor2 не менять: на них рассчитан scripts/api-check.ps1 (пн-пт 09:00-18:00, общий предмет «Математика»)
        Tutor t1 = tutor(t1u, "Математика и физика, подготовка к ЕГЭ и ОГЭ. Объясняю просто, от базы до олимпиадных задач.",
                8, "https://i.pravatar.cc/300?img=47");
        offer(t1, math, "1500");
        offer(t1, physics, "1800");
        window(t1, 9, 18, 1, 2, 3, 4, 5);

        Tutor t2 = tutor(t2u, "Английский и немецкий для школьников и взрослых, подготовка к IELTS, помощь с математикой в 5-9 классах.",
                5, "https://i.pravatar.cc/300?img=12");
        offer(t2, english, "1200");
        offer(t2, german, "1300");
        offer(t2, math, "1300");
        window(t2, 9, 18, 1, 2, 3, 4, 5);

        Tutor t3 = tutor(t3u, "Химия и биология для поступающих в медицинские вузы. Разбираю сложные темы на опытах и схемах.",
                12, "https://i.pravatar.cc/300?img=32");
        offer(t3, chemistry, "1600");
        offer(t3, biology, "1400");
        window(t3, 10, 14, 1, 3, 5);       // пн, ср, пт: два окна с перерывом
        window(t3, 16, 20, 1, 3, 5);
        window(t3, 10, 15, 6);             // суббота

        Tutor t4 = tutor(t4u, "Программист-практик. Информатика, Python, подготовка к ЕГЭ и олимпиадам, физика для старших классов.",
                10, "https://i.pravatar.cc/300?img=15");
        offer(t4, informatics, "2000");
        offer(t4, physics, "1700");
        window(t4, 15, 21, 1, 2, 3, 4, 5); // вечерние занятия
        window(t4, 10, 16, 6, 7);          // и выходные

        Tutor t5 = tutor(t5u, "Филолог с 15-летним стажем. Русский язык, литература и история: от сочинений до ЕГЭ.",
                15, "https://i.pravatar.cc/300?img=44");
        offer(t5, russian, "1100");
        offer(t5, literature, "1000");
        offer(t5, history, "1200");
        window(t5, 9, 13, 2, 4);           // вт, чт: два окна с обеденным перерывом
        window(t5, 14, 18, 2, 4);
        window(t5, 12, 17, 1, 3, 5);       // пн, ср, пт

        // ===================== проведённые занятия: оценки в обе стороны и домашние задания =====================
        completed(st1, t1, math, 7, 10, 60, 5, 5, "Решить задачи 1-10 из сборника");
        completed(st1, t1, math, 3, 12, 60, 4, 5, "Повторить тему «Производная»");
        completed(st1, t2, english, 8, 15, 45, 5, 5, "Выучить 20 новых слов (Unit 4)");
        completed(st2, t3, chemistry, 6, 16, 60, 5, 4, "Выучить таблицу растворимости");
        completed(st2, t3, biology, 2, 17, 60, 5, 5, "Подготовить доклад по теме «Клетка»");
        completed(st2, t2, math, 12, 16, 60, 4, 3, null);
        completed(st3, t4, informatics, 5, 18, 90, 5, 5, "Написать на Python программу сортировки");
        completed(st3, t4, physics, 1, 19, 60, 4, 4, "Задачи по кинематике №5-12");
        completed(st3, t2, german, 9, 11, 60, 4, 5, "Выучить спряжение глаголов sein и haben");
        completedIntro(st3, t5, history, 6, 12, 30, 5, 5);                  // вводное занятие (бесплатное)
        completed(st4, t5, russian, 4, 13, 60, 5, 5, "Упражнения 15-20 на правописание н/нн");
        completed(st4, t5, literature, 2, 14, 60, 4, 5, "Прочитать главы 1-3 «Евгения Онегина»");
        completed(st4, t1, physics, 10, 14, 90, 5, 5, "Конспект по теме «Законы Ньютона»");

        // ===================== отменённые и отклонённые (вкладка «Прошлые») =====================
        past(st1, t2, english, 1, 15, 45, BookingStatus.CANCELLED);
        past(st3, t5, history, 3, 11, 60, BookingStatus.REJECTED);

        // ===================== предстоящие: подтверждённые и ожидающие ответа репетитора =====================
        LocalDate workday = nextOn(1, 2, 3, 4, 5);
        upcoming(st1, t2, english, workday, 14, 45, BookingStatus.CONFIRMED);
        upcoming(st2, t1, physics, workday, 11, 90, BookingStatus.PENDING);
        upcoming(st3, t4, informatics, workday, 17, 60, BookingStatus.CONFIRMED);
        upcoming(st4, t5, literature, workday, 14, 60, BookingStatus.PENDING);
        upcoming(st2, t3, chemistry, nextOn(1, 3, 5), 16, 60, BookingStatus.CONFIRMED);
    }

    // ---------------------------------------------------------------- пользователи, каталог, график

    private User user(String email, String pwd, String name, Role role, LocalDate birth) {
        User u = new User();
        u.setEmail(email);
        u.setPasswordHash(encoder.encode(pwd));
        u.setFullName(name);
        u.setRole(role);
        u.setBirthDate(birth);
        u.setPhone(String.format("+7 900 000-00-%02d", ++phoneSeq));
        return users.save(u);
    }

    private Subject subject(String name) {
        Subject s = new Subject();
        s.setName(name);
        return subjects.save(s);
    }

    private Tutor tutor(User user, String bio, int experienceYears, String photoUrl) {
        Tutor t = new Tutor();
        t.setUser(user);
        t.setBio(bio);
        t.setExperienceYears(experienceYears);
        t.setPhotoUrl(photoUrl);
        return tutors.save(t);
    }

    private void offer(Tutor t, Subject s, String pricePerHour) {
        TutorSubject o = new TutorSubject();
        o.setTutor(t);
        o.setSubject(s);
        o.setPricePerHour(new BigDecimal(pricePerHour));
        offers.save(o);
        prices.put(t.getId() + ":" + s.getId(), o.getPricePerHour());
    }

    /** Рабочее окно [from, to) в указанные дни недели (1 = понедельник ... 7 = воскресенье). */
    private void window(Tutor t, int from, int to, int... days) {
        for (int day : days) {
            WorkingHours w = new WorkingHours();
            w.setTutorId(t.getId());
            w.setDayOfWeek(day);
            w.setStartTime(LocalTime.of(from, 0));
            w.setEndTime(LocalTime.of(to, 0));
            hours.save(w);
        }
    }

    // ---------------------------------------------------------------- записи

    private void completed(User st, Tutor t, Subject s, int daysAgo, int hour, int minutes,
                           int tutorRating, int studentRating, String homework) {
        save(st, t, s, at(today().minusDays(daysAgo), hour), minutes, BookingStatus.COMPLETED, false,
                tutorRating, studentRating, homework);
    }

    private void completedIntro(User st, Tutor t, Subject s, int daysAgo, int hour, int minutes,
                                int tutorRating, int studentRating) {
        save(st, t, s, at(today().minusDays(daysAgo), hour), minutes, BookingStatus.COMPLETED, true,
                tutorRating, studentRating, null);
    }

    private void past(User st, Tutor t, Subject s, int daysAgo, int hour, int minutes, BookingStatus status) {
        save(st, t, s, at(today().minusDays(daysAgo), hour), minutes, status, false, null, null, null);
    }

    private void upcoming(User st, Tutor t, Subject s, LocalDate date, int hour, int minutes, BookingStatus status) {
        save(st, t, s, at(date, hour), minutes, status, false, null, null, null);
    }

    private void save(User student, Tutor tutor, Subject subject, Instant start, int minutes, BookingStatus status,
                      boolean intro, Integer tutorRating, Integer studentRating, String homework) {
        Booking b = new Booking();
        b.setStudent(student);
        b.setTutor(tutor);
        b.setSubject(subject);
        b.setStartTime(start);
        b.setEndTime(start.plus(Duration.ofMinutes(minutes)));
        b.setStatus(status);
        b.setIntro(intro);
        b.setPrice(intro ? BigDecimal.ZERO.setScale(2) : priceOf(tutor, subject, minutes));
        b.setTutorRating(tutorRating);
        b.setStudentRating(studentRating);
        b.setHomework(homework);
        b.setCreatedAt(clock.instant());
        if (status == BookingStatus.CANCELLED) b.setCancelledAt(clock.instant());
        bookings.save(b);
    }

    // ---------------------------------------------------------------- время и цена

    private BigDecimal priceOf(Tutor t, Subject s, int minutes) {
        return prices.get(t.getId() + ":" + s.getId())
                .multiply(BigDecimal.valueOf(minutes))
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }

    private LocalDate today() { return LocalDate.now(props.timezone()); }

    private Instant at(LocalDate d, int hour) { return d.atTime(hour, 0).atZone(props.timezone()).toInstant(); }

    /** Ближайший день (начиная с завтра), чей номер дня недели есть в списке. */
    private LocalDate nextOn(int... days) {
        LocalDate d = today().plusDays(1);
        while (!contains(days, d.getDayOfWeek().getValue())) d = d.plusDays(1);
        return d;
    }

    private boolean contains(int[] days, int value) {
        for (int x : days) if (x == value) return true;
        return false;
    }
}