package ru.vsuet.team.tutoringbackend.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record RegisterRequest(@NotBlank @Email @Size(max = 255) String email,
                              @NotBlank @Size(min = 8, max = 72) String password,
                              @NotBlank @Size(max = 255) String fullName,
                              @Size(max = 32) String phone,
                              @Past LocalDate birthDate) {
}
