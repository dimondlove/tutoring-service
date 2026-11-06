package ru.vsuet.team.tutoringbackend.booking.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record CreateBookingRequest(@NotNull Long tutorId, @NotNull Long subjectId, @NotNull Instant startTime,
                                   @Min(1) int durationMinutes, boolean intro) {}
