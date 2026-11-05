package ru.vsuet.team.tutoringbackend.auth;

import ru.vsuet.team.tutoringbackend.user.Role;

public record AuthUser(Long id, String email, Role role) {
}
