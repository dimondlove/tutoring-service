package ru.vsuet.team.tutoringbackend.catalog.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.vsuet.team.tutoringbackend.booking.BookingRepository;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RatingService {
    private final BookingRepository bookings;

    public record Stats(double average, long count) {
        public static final Stats NONE = new Stats(0, 0);
    }

    @Transactional(readOnly = true)
    public Map<Long, Stats> tutorStats(Collection<Long> tutorIds) {
        return tutorIds.isEmpty() ? Map.of() : toMap(bookings.tutorRatingStats(tutorIds));
    }

    @Transactional(readOnly = true)
    public Map<Long, Stats> studentStats(Collection<Long> studentIds) {
        return studentIds.isEmpty() ? Map.of() : toMap(bookings.studentRatingStats(studentIds));
    }

    private Map<Long, Stats> toMap(List<Object[]> rows) {
        Map<Long, Stats> m = new HashMap<>();
        for (Object[] r : rows) {
            double avg = Math.round(((Number) r[1]).doubleValue() * 10) / 10.0;
            m.put(((Number) r[0]).longValue(), new Stats(avg, ((Number) r[2]).longValue()));
        }
        return m;
    }
}
