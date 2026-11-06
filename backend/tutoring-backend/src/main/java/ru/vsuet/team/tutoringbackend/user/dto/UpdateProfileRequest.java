package ru.vsuet.team.tutoringbackend.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateProfileRequest(@NotBlank @Size(max = 255) String fullName, @Size(max = 32) String phone,
                                   @Past LocalDate birthDate
) {
}
