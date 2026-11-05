package ru.vsuet.team.tutoringbackend.schedule.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record UpdateWorkingHoursRequest(@NotNull @Valid List<WorkingHoursDto> items) {}