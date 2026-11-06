package ru.vsuet.team.tutoringbackend.schedule.dto;

import ru.vsuet.team.tutoringbackend.schedule.entity.TimeOff;

import java.time.Instant;

public record TimeOffResponse(Long id, Instant startAt, Instant endAt, String reason) {
    public static TimeOffResponse from(TimeOff t) {
        return new TimeOffResponse(t.getId(), t.getStartAt(), t.getEndAt(), t.getReason());
    }
}
