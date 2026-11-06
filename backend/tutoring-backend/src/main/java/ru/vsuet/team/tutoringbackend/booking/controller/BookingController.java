package ru.vsuet.team.tutoringbackend.booking.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ru.vsuet.team.tutoringbackend.auth.AuthUser;
import ru.vsuet.team.tutoringbackend.booking.service.BookingService;
import ru.vsuet.team.tutoringbackend.booking.dto.BookingResponse;
import ru.vsuet.team.tutoringbackend.booking.dto.CreateBookingRequest;
import ru.vsuet.team.tutoringbackend.booking.dto.RatingRequest;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {
    private final BookingService bookings;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('STUDENT')")
    public BookingResponse create(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody CreateBookingRequest r) {
        return bookings.create(me.id(), r);
    }

    @GetMapping("/my/upcoming") @PreAuthorize("hasRole('STUDENT')")
    public List<BookingResponse> upcoming(@AuthenticationPrincipal AuthUser me) { return bookings.upcoming(me.id()); }

    @GetMapping("/my/past") @PreAuthorize("hasRole('STUDENT')")
    public List<BookingResponse> past(@AuthenticationPrincipal AuthUser me) { return bookings.past(me.id()); }

    @PostMapping("/{id}/cancel") @PreAuthorize("hasAnyRole('STUDENT','ADMIN')")
    public BookingResponse cancel(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        return bookings.cancel(id, me);          // владелец проверяется внутри сервиса
    }

    @PostMapping("/{id}/rate") @PreAuthorize("hasRole('STUDENT')")
    public BookingResponse rate(@AuthenticationPrincipal AuthUser me, @PathVariable Long id,
                                @Valid @RequestBody RatingRequest r) {
        return bookings.rateTutor(id, me.id(), r.rating());
    }
}
