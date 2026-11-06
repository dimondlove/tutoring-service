package ru.vsuet.team.tutoringbackend.support;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import ru.vsuet.team.tutoringbackend.auth.service.JwtService;
import ru.vsuet.team.tutoringbackend.catalog.entity.Subject;
import ru.vsuet.team.tutoringbackend.catalog.entity.Tutor;
import ru.vsuet.team.tutoringbackend.catalog.entity.TutorSubject;
import ru.vsuet.team.tutoringbackend.catalog.repository.SubjectRepository;
import ru.vsuet.team.tutoringbackend.catalog.repository.TutorRepository;
import ru.vsuet.team.tutoringbackend.catalog.repository.TutorSubjectRepository;
import ru.vsuet.team.tutoringbackend.config.AppProperties;
import ru.vsuet.team.tutoringbackend.schedule.entity.WorkingHours;
import ru.vsuet.team.tutoringbackend.schedule.repository.WorkingHoursRepository;
import ru.vsuet.team.tutoringbackend.user.Role;
import ru.vsuet.team.tutoringbackend.user.User;
import ru.vsuet.team.tutoringbackend.user.repository.UserRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

@Component
@RequiredArgsConstructor
public class TestFixtures {
    private final UserRepository users;
    private final SubjectRepository subjects;
    private final TutorRepository tutors;
    private final TutorSubjectRepository offers;
    private final WorkingHoursRepository hours;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final AppProperties props;

    public User user(String email, Role role) {
        User u = new User();
        u.setEmail(email); u.setFullName("User " + email); u.setRole(role);
        u.setPasswordHash(encoder.encode("Password1!"));
        return users.save(u);
    }

    public Subject subject(String name) { Subject s = new Subject(); s.setName(name); return subjects.save(s); }

    /** Репетитор: предметы по 1000 ₽/час, график 09:00–18:00 каждый день. */
    public Tutor tutor(String email, Subject... taught) {
        Tutor t = new Tutor();
        t.setUser(user(email, Role.TUTOR));
        t.setExperienceYears(5);
        t = tutors.save(t);
        for (Subject s : taught) {
            TutorSubject o = new TutorSubject();
            o.setTutor(t); o.setSubject(s); o.setPricePerHour(new BigDecimal("1000.00"));
            offers.save(o);
        }
        for (int day = 1; day <= 7; day++) {
            WorkingHours w = new WorkingHours();
            w.setTutorId(t.getId()); w.setDayOfWeek(day);
            w.setStartTime(LocalTime.of(9, 0)); w.setEndTime(LocalTime.of(18, 0));
            hours.save(w);
        }
        return t;
    }

    public String token(User u) { return jwt.generate(u); }

    public Instant slot(int daysAhead, int hour, int minute) {
        ZoneId zone = props.timezone();
        return LocalDate.now(zone).plusDays(daysAhead).atTime(hour, minute).atZone(zone).toInstant();
    }
}
