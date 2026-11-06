package ru.vsuet.team.tutoringbackend.schedule.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.vsuet.team.tutoringbackend.booking.repository.BookingRepository;
import ru.vsuet.team.tutoringbackend.catalog.repository.TutorRepository;
import ru.vsuet.team.tutoringbackend.common.BadRequestException;
import ru.vsuet.team.tutoringbackend.common.ConflictException;
import ru.vsuet.team.tutoringbackend.common.NotFoundException;
import ru.vsuet.team.tutoringbackend.schedule.dto.TimeOffRequest;
import ru.vsuet.team.tutoringbackend.schedule.dto.TimeOffResponse;
import ru.vsuet.team.tutoringbackend.schedule.dto.WorkingHoursDto;
import ru.vsuet.team.tutoringbackend.schedule.entity.TimeOff;
import ru.vsuet.team.tutoringbackend.schedule.repository.TimeOffRepository;
import ru.vsuet.team.tutoringbackend.schedule.entity.WorkingHours;
import ru.vsuet.team.tutoringbackend.schedule.repository.WorkingHoursRepository;

import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScheduleManagementService {
    private final TutorRepository tutors;
    private final WorkingHoursRepository hours;
    private final TimeOffRepository timeOffs;
    private final BookingRepository bookings;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<WorkingHoursDto> getWorkingHours(Long tutorId) {
        return hours.findByTutorIdOrderByDayOfWeekAscStartTimeAsc(tutorId).stream()
                .map(w -> new WorkingHoursDto(w.getDayOfWeek(), w.getStartTime(), w.getEndTime())).toList();
    }

    @Transactional   // уже созданные записи при смене графика не трогаются
    public List<WorkingHoursDto> replaceWorkingHours(Long tutorId, List<WorkingHoursDto> items) {
        tutors.findById(tutorId).orElseThrow(() -> new NotFoundException("TUTOR_NOT_FOUND", "Репетитор не найден"));
        for (WorkingHoursDto i : items)
            if (!i.endTime().isAfter(i.startTime()))
                throw new BadRequestException("BAD_WORKING_HOURS", "Конец окна должен быть позже начала");
        items.stream().collect(Collectors.groupingBy(WorkingHoursDto::dayOfWeek)).forEach((day, list) -> {
            var sorted = list.stream().sorted(Comparator.comparing(WorkingHoursDto::startTime)).toList();
            for (int k = 1; k < sorted.size(); k++)
                if (sorted.get(k).startTime().isBefore(sorted.get(k - 1).endTime()))
                    throw new BadRequestException("OVERLAPPING_WORKING_HOURS", "Окна в один день пересекаются");
        });
        hours.deleteAllForTutor(tutorId);
        items.forEach(i -> {
            WorkingHours w = new WorkingHours();
            w.setTutorId(tutorId); w.setDayOfWeek(i.dayOfWeek());
            w.setStartTime(i.startTime()); w.setEndTime(i.endTime());
            hours.save(w);
        });
        return getWorkingHours(tutorId);
    }

    @Transactional(readOnly = true)
    public List<TimeOffResponse> listTimeOff(Long tutorId) {
        return timeOffs.findByTutorIdAndEndAtAfterOrderByStartAt(tutorId, clock.instant())
                .stream().map(TimeOffResponse::from).toList();
    }

    @Transactional
    public TimeOffResponse addTimeOff(Long tutorId, TimeOffRequest r) {
        if (!r.endAt().isAfter(r.startAt()))
            throw new BadRequestException("BAD_RANGE", "Конец должен быть позже начала");
        // FOR UPDATE: пока идёт создание записи этого репетитора, ждём; новые записи ждут нас
        tutors.findByIdForUpdate(tutorId)
                .orElseThrow(() -> new NotFoundException("TUTOR_NOT_FOUND", "Репетитор не найден"));
        if (bookings.existsActiveForTutor(tutorId, r.startAt(), r.endAt()))
            throw new ConflictException("TIME_OFF_HAS_BOOKINGS", "В этот период уже есть записи");
        TimeOff t = new TimeOff();
        t.setTutorId(tutorId); t.setStartAt(r.startAt()); t.setEndAt(r.endAt()); t.setReason(r.reason());
        return TimeOffResponse.from(timeOffs.save(t));
    }

    @Transactional
    public void deleteTimeOff(Long tutorId, Long timeOffId) {
        TimeOff t = timeOffs.findById(timeOffId).filter(x -> x.getTutorId().equals(tutorId))
                .orElseThrow(() -> new NotFoundException("TIME_OFF_NOT_FOUND", "Выходной не найден"));
        timeOffs.delete(t);
    }
}