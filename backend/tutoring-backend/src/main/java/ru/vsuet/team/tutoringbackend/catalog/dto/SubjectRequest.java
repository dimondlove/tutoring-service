package ru.vsuet.team.tutoringbackend.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SubjectRequest(@NotBlank @Size(max = 100) String name, Boolean active) {}
