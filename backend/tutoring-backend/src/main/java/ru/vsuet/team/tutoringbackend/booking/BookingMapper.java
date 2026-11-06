package ru.vsuet.team.tutoringbackend.booking;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.vsuet.team.tutoringbackend.catalog.service.RatingService;
import ru.vsuet.team.tutoringbackend.config.AppProperties;
import ru.vsuet.team.tutoringbackend.user.User;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.Period;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class BookingMapper {
    private final RatingService ratings;
    private final AppProperties props;
    private final Clock clock;

    public BookingResponse toResponse(Booking b) { return toResponses(List.of(b)).getFirst(); }

    public List<BookingResponse> toResponses(List<Booking> list) {
        if (list.isEmpty()) return List.of();
        var tutorStats = ratings.tutorStats(list.stream().map(b -> b.getTutor().getId()).collect(Collectors.toSet()));
        var studentStats = ratings.studentStats(list.stream().map(b -> b.getStudent().getId()).collect(Collectors.toSet()));
        LocalDate today = today();
        return list.stream().map(b -> {
            var t = b.getTutor();
            var ts = tutorStats.getOrDefault(t.getId(), RatingService.Stats.NONE);
            return new BookingResponse(b.getId(), b.getStatus(), b.isIntro(), b.getStartTime(), b.getEndTime(),
                    Duration.between(b.getStartTime(), b.getEndTime()).toMinutes(), b.getPrice(), b.getHomework(),
                    b.getTutorRating(), b.getStudentRating(), b.getCancelledAt(),
                    new BookingResponse.SubjectRef(b.getSubject().getId(), b.getSubject().getName()),
                    new BookingResponse.TutorRef(t.getId(), t.getUser().getFullName(), t.getPhotoUrl(), ts.average()),
                    studentRef(b.getStudent(), studentStats, today));
        }).toList();
    }

    public List<BookingResponse.StudentRef> toStudentRefs(Collection<User> students) {
        var stats = ratings.studentStats(students.stream().map(User::getId).toList());
        LocalDate today = today();
        return students.stream().map(u -> studentRef(u, stats, today)).toList();
    }

    private BookingResponse.StudentRef studentRef(User u, Map<Long, RatingService.Stats> stats, LocalDate today) {
        var s = stats.getOrDefault(u.getId(), RatingService.Stats.NONE);
        Integer age = u.getBirthDate() == null ? null : Period.between(u.getBirthDate(), today).getYears();
        return new BookingResponse.StudentRef(u.getId(), u.getFullName(), age, u.getPhone(), u.getEmail(),
                s.average(), s.count());
    }

    private LocalDate today() { return LocalDate.now(clock.withZone(props.timezone())); }
}
