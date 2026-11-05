package ru.vsuet.team.tutoringbackend.catalog.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record UpdateTutorProfileRequest(@Size(max = 5000) String bio, @Size(max = 500) String photoUrl,
                                        @Min(0) @Max(80) int experienceYears) {}
