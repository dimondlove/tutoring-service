package ru.vsuet.team.tutoringbackend.auth;

import ru.vsuet.team.tutoringbackend.user.UserResponse;

public record AuthResponse(String token, UserResponse user) {
}
