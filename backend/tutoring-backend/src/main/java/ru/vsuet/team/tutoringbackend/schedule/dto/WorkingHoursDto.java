package ru.vsuet.team.tutoringbackend.schedule.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public record WorkingHoursDto(@Min(1) @Max(7) int dayOfWeek, @NotNull LocalTime startTime, @NotNull LocalTime endTime) {}
