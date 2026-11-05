package ru.vsuet.team.tutoringbackend.common;

import java.util.Map;

public record ApiError(String code, String message, Map<String, String> details) {
}
