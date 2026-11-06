package ru.vsuet.team.tutoringbackend.auth.dto;

import ru.vsuet.team.tutoringbackend.user.dto.UserResponse;

public record AuthResponse(String token, UserResponse user) {
}
