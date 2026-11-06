package ru.vsuet.team.tutoringbackend.schedule.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.vsuet.team.tutoringbackend.booking.repository.BookingRepository;
import ru.vsuet.team.tutoringbackend.booking.BookingStatus;
import ru.vsuet.team.tutoringbackend.catalog.entity.Subject;
import ru.vsuet.team.tutoringbackend.catalog.repository.SubjectRepository;
import ru.vsuet.team.tutoringbackend.catalog.repository.TutorRepository;
import ru.vsuet.team.tutoringbackend.catalog.repository.TutorSubjectRepository;
import ru.vsuet.team.tutoringbackend.common.BadRequestException;
import ru.vsuet.team.tutoringbackend.common.NotFoundException;
import ru.vsuet.team.tutoringbackend.config.AppProperties;
import ru.vsuet.team.tutoringbackend.schedule.dto.SlotResponse;
import ru.vsuet.team.tutoringbackend.schedule.repository.TimeOffRepository;
import ru.vsuet.team.tutoringbackend.schedule.entity.WorkingHours;
import ru.vsuet.team.tutoringbackend.schedule.repository.WorkingHoursRepository;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.time.Clock;

@Service
@RequiredArgsConstructor
public class SlotService {
    private final TutorRepository tutors;
    private final SubjectRepository subjects;
    private final TutorSubjectRepository offers;
    private final WorkingHoursRepository workingHours;
    private final TimeOffRepository timeOffs;
    private final BookingRepository bookings;
    private final AppProperties props;
    private final Clock clock;

    private record Interval(Instant start, Instant end) {
        boolean overlaps(Instant s, Instant e) { return start.isBefore(e) && end.isAfter(s); }
    }

    @Transactional(readOnly = true)
    public List<SlotResponse> findSlots(Long tutorId, Long subjectId, int durationMinutes, LocalDate date) {
        if (!props.allowedDurations().contains(durationMinutes))
            throw new BadRequestException("BAD_DURATION", "Недопустимая длительность занятия");
        subjects.findById(subjectId).filter(Subject::isActive)
                .orElseThrow(() -> new NotFoundException("SUBJECT_NOT_FOUND", "Предмет не найден"));
        if (!tutors.existsByIdAndActiveTrue(tutorId) || !offers.existsByTutor_IdAndSubject_Id(tutorId, subjectId))
            throw new NotFoundException("TUTOR_NOT_FOUND", "Репетитор не найден или не ведёт этот предмет");

        ZoneId zone = props.timezone();
        Instant dayStart = date.atStartOfDay(zone).toInstant();
        Instant dayEnd = date.plusDays(1).atStartOfDay(zone).toInstant();

        List<Interval> busy = new ArrayList<>();
        bookings.findForTutorBetween(tutorId, dayStart, dayEnd, BookingStatus.BLOCKING)
                .forEach(b -> busy.add(new Interval(b.getStartTime(), b.getEndTime())));
        timeOffs.findOverlapping(tutorId, dayStart, dayEnd)
                .forEach(t -> busy.add(new Interval(t.getStartAt(), t.getEndAt())));

        Duration duration = Duration.ofMinutes(durationMinutes);
        Duration step = Duration.ofMinutes(props.slotStepMinutes());
        Instant now = clock.instant();
        List<SlotResponse> result = new ArrayList<>();

        for (WorkingHours wh : workingHours.findByTutorIdAndDayOfWeekOrderByStartTime(
                tutorId, date.getDayOfWeek().getValue())) {
            Instant winStart = date.atTime(wh.getStartTime()).atZone(zone).toInstant();
            Instant winEnd = date.atTime(wh.getEndTime()).atZone(zone).toInstant();
            for (Instant s = winStart; !s.plus(duration).isAfter(winEnd); s = s.plus(step)) {
                Instant from = s, to = s.plus(duration);
                if (from.isAfter(now) && busy.stream().noneMatch(b -> b.overlaps(from, to)))
                    result.add(new SlotResponse(from, to));
            }
        }
        return result;
    }

    /** Лежит ли интервал в рабочем окне, на сетке и вне выходных. Используется при создании записи. */
    public boolean fitsSchedule(Long tutorId, Instant start, Instant end) {
        ZoneId zone = props.timezone();
        LocalDate date = start.atZone(zone).toLocalDate();
        if (!date.equals(end.atZone(zone).toLocalDate())) return false;

        boolean inWindow = workingHours
                .findByTutorIdAndDayOfWeekOrderByStartTime(tutorId, date.getDayOfWeek().getValue())
                .stream().anyMatch(wh -> {
                    Instant winStart = date.atTime(wh.getStartTime()).atZone(zone).toInstant();
                    Instant winEnd = date.atTime(wh.getEndTime()).atZone(zone).toInstant();
                    return !start.isBefore(winStart) && !end.isAfter(winEnd)
                            && Duration.between(winStart, start).toMinutes() % props.slotStepMinutes() == 0;
                });
        return inWindow && timeOffs.findOverlapping(tutorId, start, end).isEmpty();
    }
}
