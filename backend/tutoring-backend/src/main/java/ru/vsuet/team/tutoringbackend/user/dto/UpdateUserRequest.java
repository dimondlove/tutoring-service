package ru.vsuet.team.tutoringbackend.user.dto;

import ru.vsuet.team.tutoringbackend.user.Role;

public record UpdateUserRequest(Role role, Boolean enabled) {
}
