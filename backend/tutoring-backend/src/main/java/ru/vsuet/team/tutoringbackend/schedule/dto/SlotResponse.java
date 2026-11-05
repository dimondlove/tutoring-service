package ru.vsuet.team.tutoringbackend.schedule.dto;

import java.time.Instant;

public record SlotResponse(Instant start, Instant end) {}
