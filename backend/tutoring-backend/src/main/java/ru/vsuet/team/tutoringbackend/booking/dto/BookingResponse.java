package ru.vsuet.team.tutoringbackend.booking.dto;

import ru.vsuet.team.tutoringbackend.booking.BookingStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record BookingResponse(Long id, BookingStatus status, boolean intro, Instant startTime, Instant endTime,
                              long durationMinutes, BigDecimal price, String homework,
                              Integer tutorRating, Integer studentRating, Instant cancelledAt,
                              SubjectRef subject, TutorRef tutor, StudentRef student) {

    public record SubjectRef(Long id, String name) {}

    public record TutorRef(Long id, String fullName, String photoUrl, double rating) {}

    public record StudentRef(Long id, String fullName, Integer age, String phone, String email,
                             double rating, long ratingCount) {}

}
