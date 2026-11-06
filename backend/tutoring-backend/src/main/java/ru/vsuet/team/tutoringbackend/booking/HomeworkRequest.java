package ru.vsuet.team.tutoringbackend.booking;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record HomeworkRequest(@NotBlank @Size(max = 5000) String homework) {}
