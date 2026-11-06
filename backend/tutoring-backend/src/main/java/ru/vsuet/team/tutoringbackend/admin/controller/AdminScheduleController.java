package ru.vsuet.team.tutoringbackend.admin.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ru.vsuet.team.tutoringbackend.auth.AuthUser;
import ru.vsuet.team.tutoringbackend.booking.dto.BookingResponse;
import ru.vsuet.team.tutoringbackend.booking.service.BookingService;
import ru.vsuet.team.tutoringbackend.booking.BookingStatus;
import ru.vsuet.team.tutoringbackend.schedule.service.ScheduleManagementService;
import ru.vsuet.team.tutoringbackend.schedule.dto.TimeOffRequest;
import ru.vsuet.team.tutoringbackend.schedule.dto.TimeOffResponse;
import ru.vsuet.team.tutoringbackend.schedule.dto.UpdateWorkingHoursRequest;
import ru.vsuet.team.tutoringbackend.schedule.dto.WorkingHoursDto;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminScheduleController {
    private final BookingService bookings;
    private final ScheduleManagementService schedule;

    @GetMapping("/schedule")                     // общее расписание, фильтры: репетитор, статусы
    public List<BookingResponse> schedule(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long tutorId,
            @RequestParam(required = false) Set<BookingStatus> status) {
        return bookings.schedule(tutorId, from, to, status);
    }

    @PostMapping("/bookings/{id}/cancel")        // для ADMIN нет ограничения по сроку
    public BookingResponse cancel(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        return bookings.cancel(id, me);
    }

    @GetMapping("/tutors/{id}/working-hours")
    public List<WorkingHoursDto> workingHours(@PathVariable Long id) { return schedule.getWorkingHours(id); }

    @PutMapping("/tutors/{id}/working-hours")
    public List<WorkingHoursDto> setWorkingHours(@PathVariable Long id, @Valid @RequestBody UpdateWorkingHoursRequest r) {
        return schedule.replaceWorkingHours(id, r.items());
    }

    @PostMapping("/tutors/{id}/time-off")
    @ResponseStatus(HttpStatus.CREATED)
    public TimeOffResponse addTimeOff(@PathVariable Long id, @Valid @RequestBody TimeOffRequest r) {
        return schedule.addTimeOff(id, r);
    }
}
