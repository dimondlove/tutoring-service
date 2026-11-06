package ru.vsuet.team.tutoringbackend.tutor;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ru.vsuet.team.tutoringbackend.auth.AuthUser;
import ru.vsuet.team.tutoringbackend.booking.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/tutor")
@RequiredArgsConstructor
public class TutorLessonsController {
    private final BookingService bookings;
    private final TutorProfileService profile;

    @GetMapping("/lessons/today")                 // главная: занятия сегодня
    public List<BookingResponse> today(@AuthenticationPrincipal AuthUser me) {
        return bookings.today(profile.tutorIdOf(me.id()));
    }

    @GetMapping("/schedule")                      // занятия по датам
    public List<BookingResponse> schedule(
            @AuthenticationPrincipal AuthUser me,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return bookings.schedule(profile.tutorIdOf(me.id()), from, to, BookingStatus.BLOCKING);
    }

    @GetMapping("/requests")                      // «Мои ученики»: заявки с карточкой ученика
    public List<BookingResponse> requests(@AuthenticationPrincipal AuthUser me) {
        return bookings.requests(profile.tutorIdOf(me.id()));
    }

    @GetMapping("/students")                      // все ученики репетитора
    public List<BookingResponse.StudentRef> students(@AuthenticationPrincipal AuthUser me) {
        return bookings.students(profile.tutorIdOf(me.id()));
    }

    @PostMapping("/bookings/{id}/accept")
    public BookingResponse accept(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        return bookings.accept(id, me);
    }

    @PostMapping("/bookings/{id}/decline")
    public BookingResponse decline(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        return bookings.decline(id, me);
    }

    @PutMapping("/bookings/{id}/homework")
    public BookingResponse homework(@AuthenticationPrincipal AuthUser me, @PathVariable Long id,
                                    @Valid @RequestBody HomeworkRequest r) {
        return bookings.setHomework(id, me, r.homework());
    }

    @PostMapping("/bookings/{id}/rate-student")
    public BookingResponse rateStudent(@AuthenticationPrincipal AuthUser me, @PathVariable Long id,
                                       @Valid @RequestBody RatingRequest r) {
        return bookings.rateStudent(id, me, r.rating());
    }
}
