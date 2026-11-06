package ru.vsuet.team.tutoringbackend.schedule;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.vsuet.team.tutoringbackend.booking.Booking;
import ru.vsuet.team.tutoringbackend.booking.repository.BookingRepository;
import ru.vsuet.team.tutoringbackend.catalog.entity.Subject;
import ru.vsuet.team.tutoringbackend.catalog.repository.SubjectRepository;
import ru.vsuet.team.tutoringbackend.catalog.repository.TutorRepository;
import ru.vsuet.team.tutoringbackend.catalog.repository.TutorSubjectRepository;
import ru.vsuet.team.tutoringbackend.common.BadRequestException;
import ru.vsuet.team.tutoringbackend.config.AppProperties;
import ru.vsuet.team.tutoringbackend.schedule.dto.SlotResponse;
import ru.vsuet.team.tutoringbackend.schedule.entity.WorkingHours;
import ru.vsuet.team.tutoringbackend.schedule.repository.TimeOffRepository;
import ru.vsuet.team.tutoringbackend.schedule.repository.WorkingHoursRepository;
import ru.vsuet.team.tutoringbackend.schedule.service.SlotService;

import java.time.*;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SlotServiceTest {
    @Mock
    TutorRepository tutors;
    @Mock
    SubjectRepository subjects;
    @Mock
    TutorSubjectRepository offers;
    @Mock
    WorkingHoursRepository workingHours;
    @Mock
    TimeOffRepository timeOffs;
    @Mock
    BookingRepository bookings;

    AppProperties props = new AppProperties(ZoneId.of("Europe/Moscow"), 30, List.of(30, 60), 30, 2,
            new AppProperties.Jwt("x".repeat(32), 60), new AppProperties.Cors(List.of()));
    Clock clock = Clock.fixed(Instant.parse("2030-01-01T00:00:00Z"), ZoneOffset.UTC);

    @Test
    void busyIntervalRemovesOverlappingSlots() {
        when(subjects.findById(1L)).thenReturn(Optional.of(new Subject()));
        when(tutors.existsByIdAndActiveTrue(1L)).thenReturn(true);
        when(offers.existsByTutor_IdAndSubject_Id(1L, 1L)).thenReturn(true);

        var wh = new WorkingHours();
        wh.setStartTime(LocalTime.of(9, 0)); wh.setEndTime(LocalTime.of(12, 0));
        when(workingHours.findByTutorIdAndDayOfWeekOrderByStartTime(1L, 1)).thenReturn(List.of(wh)); // 2030-01-07 — понедельник

        var booking = new Booking();      // 10:00–11:00 по Москве
        booking.setStartTime(Instant.parse("2030-01-07T07:00:00Z"));
        booking.setEndTime(Instant.parse("2030-01-07T08:00:00Z"));
        when(bookings.findForTutorBetween(eq(1L), any(), any(), any())).thenReturn(List.of(booking));
        when(timeOffs.findOverlapping(eq(1L), any(), any())).thenReturn(List.of());

        var sut = new SlotService(tutors, subjects, offers, workingHours, timeOffs, bookings, props, clock);
        var slots = sut.findSlots(1L, 1L, 60, LocalDate.of(2030, 1, 7));

        // остаются 09:00 и 11:00 (смежные с занятым окном)
        assertThat(slots).extracting(SlotResponse::start).containsExactly(
                Instant.parse("2030-01-07T06:00:00Z"), Instant.parse("2030-01-07T08:00:00Z"));
    }

    @Test
    void notAllowedDurationIsRejected() {
        var sut = new SlotService(tutors, subjects, offers, workingHours, timeOffs, bookings, props, clock);
        assertThatThrownBy(() -> sut.findSlots(1L, 1L, 50, LocalDate.of(2030, 1, 7)))
                .isInstanceOf(BadRequestException.class);
    }
}
