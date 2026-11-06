package ru.vsuet.team.tutoringbackend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.vsuet.team.tutoringbackend.booking.Booking;
import ru.vsuet.team.tutoringbackend.booking.BookingRepository;
import ru.vsuet.team.tutoringbackend.booking.BookingStatus;
import ru.vsuet.team.tutoringbackend.catalog.entity.Subject;
import ru.vsuet.team.tutoringbackend.catalog.entity.Tutor;
import ru.vsuet.team.tutoringbackend.catalog.entity.TutorSubject;
import ru.vsuet.team.tutoringbackend.catalog.repository.SubjectRepository;
import ru.vsuet.team.tutoringbackend.catalog.repository.TutorRepository;
import ru.vsuet.team.tutoringbackend.catalog.repository.TutorSubjectRepository;
import ru.vsuet.team.tutoringbackend.schedule.WorkingHours;
import ru.vsuet.team.tutoringbackend.schedule.WorkingHoursRepository;
import ru.vsuet.team.tutoringbackend.user.Role;
import ru.vsuet.team.tutoringbackend.user.User;
import ru.vsuet.team.tutoringbackend.user.UserRepository;

import java.math.BigDecimal;
import java.time.*;

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

    @Override
    @Transactional
    public void run(String... args) {
        if (users.count() > 0) return;     // не трогаем БД, восстановленную из дампа

        user("admin@demo.com", "Admin123!", "Администратор", Role.ADMIN, null);
        User t1u = user("tutor1@demo.com", "Tutor123!", "Анна Смирнова", Role.TUTOR, null);
        User t2u = user("tutor2@demo.com", "Tutor123!", "Дмитрий Орлов", Role.TUTOR, null);
        User st1 = user("student@demo.com", "Student123!", "Иван Петров", Role.STUDENT, LocalDate.of(2009, 5, 14));
        User st2 = user("student2@demo.com", "Student123!", "Мария Сидорова", Role.STUDENT, LocalDate.of(2007, 9, 2));

        Subject math = subject("Математика");
        Subject physics = subject("Физика");
        Subject english = subject("Английский язык");

        Tutor t1 = tutor(t1u, "Математика и физика, подготовка к ЕГЭ и ОГЭ. Объясняю просто.", 8,
                "https://i.pravatar.cc/300?img=47");
        offer(t1, math, "1500"); offer(t1, physics, "1800");
        Tutor t2 = tutor(t2u, "Разговорный английский, подготовка к IELTS.", 5, "https://i.pravatar.cc/300?img=12");
        offer(t2, english, "1200"); offer(t2, math, "1300");

        // примеры для вкладок «Предстоящие/Прошлые» и рейтингов
        booking(st1, t1, math, at(LocalDate.now(props.timezone()).minusDays(7), 10), 60,
                BookingStatus.COMPLETED, "1500.00", 5, 5, "Решить задачи 1-10 из сборника");
        booking(st1, t1, math, at(LocalDate.now(props.timezone()).minusDays(3), 12), 60,
                BookingStatus.COMPLETED, "1500.00", 4, 5, "Повторить тему «Производная»");
        booking(st1, t2, english, at(LocalDate.now(props.timezone()).minusDays(1), 15), 45,
                BookingStatus.CANCELLED, "900.00", null, null, null);
        LocalDate next = nextWorkday();
        booking(st1, t2, english, at(next, 14), 45, BookingStatus.CONFIRMED, "900.00", null, null, null);
        booking(st2, t1, physics, at(next, 11), 90, BookingStatus.PENDING, "2700.00", null, null, null);
    }

    private User user(String email, String pwd, String name, Role role, LocalDate birth) {
        User u = new User();
        u.setEmail(email); u.setPasswordHash(encoder.encode(pwd));
        u.setFullName(name); u.setRole(role); u.setBirthDate(birth);
        u.setPhone("+7 900 000-00-0" + (email.length() % 10));
        return users.save(u);
    }

    private Subject subject(String name) { Subject s = new Subject(); s.setName(name); return subjects.save(s); }

    private Tutor tutor(User user, String bio, int exp, String photo) {
        Tutor t = new Tutor();
        t.setUser(user); t.setBio(bio); t.setExperienceYears(exp); t.setPhotoUrl(photo);
        t = tutors.save(t);
        for (int day = 1; day <= 5; day++) {       // пн–пт 09:00–18:00
            WorkingHours w = new WorkingHours();
            w.setTutorId(t.getId()); w.setDayOfWeek(day);
            w.setStartTime(LocalTime.of(9, 0)); w.setEndTime(LocalTime.of(18, 0));
            hours.save(w);
        }
        return t;
    }

    private void offer(Tutor t, Subject s, String price) {
        TutorSubject o = new TutorSubject();
        o.setTutor(t); o.setSubject(s); o.setPricePerHour(new BigDecimal(price));
        offers.save(o);
    }

    private void booking(User student, Tutor tutor, Subject subject, Instant start, int minutes, BookingStatus status,
                         String price, Integer tutorRating, Integer studentRating, String homework) {
        Booking b = new Booking();
        b.setStudent(student); b.setTutor(tutor); b.setSubject(subject);
        b.setStartTime(start); b.setEndTime(start.plus(Duration.ofMinutes(minutes)));
        b.setStatus(status); b.setPrice(new BigDecimal(price));
        b.setTutorRating(tutorRating); b.setStudentRating(studentRating); b.setHomework(homework);
        b.setCreatedAt(clock.instant());
        if (status == BookingStatus.CANCELLED) b.setCancelledAt(clock.instant());
        bookings.save(b);
    }

    private Instant at(LocalDate d, int hour) { return d.atTime(hour, 0).atZone(props.timezone()).toInstant(); }

    private LocalDate nextWorkday() {
        LocalDate d = LocalDate.now(props.timezone()).plusDays(1);
        while (d.getDayOfWeek().getValue() > 5) d = d.plusDays(1);
        return d;
    }
}
