package ru.vsuet.team.tutoringbackend.user;

import java.time.LocalDate;

public record UserResponse(Long id, String email, String fullName, String phone, LocalDate birthDate,
                           Role role, boolean enabled) {
    public static UserResponse from(User u) {
        return new UserResponse(u.getId(), u.getEmail(), u.getFullName(), u.getPhone(),
                u.getBirthDate(), u.getRole(), u.isEnabled());
    }
}
