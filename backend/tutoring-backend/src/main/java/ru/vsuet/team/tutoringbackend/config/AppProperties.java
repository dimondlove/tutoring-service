package ru.vsuet.team.tutoringbackend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.ZoneId;
import java.util.List;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        ZoneId timezone, int slotStepMinutes, List<Integer> allowedDurations, int introDurationMinutes,
        int minCancelHours, Jwt jwt, Cors cors
        ) {
    public record Jwt(String secret, long ttlMinutes) {}
    public record Cors(List<String> allowedOrigins) {}
}
