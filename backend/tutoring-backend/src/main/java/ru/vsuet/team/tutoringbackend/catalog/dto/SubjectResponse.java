package ru.vsuet.team.tutoringbackend.catalog.dto;

import ru.vsuet.team.tutoringbackend.catalog.entity.Subject;

public record SubjectResponse(Long id, String name, boolean active) {
    public static SubjectResponse from(Subject subject) {
        return new SubjectResponse(subject.getId(), subject.getName(), subject.isActive());
    }
}
