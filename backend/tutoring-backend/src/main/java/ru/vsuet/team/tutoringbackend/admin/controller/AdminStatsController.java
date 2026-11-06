package ru.vsuet.team.tutoringbackend.admin.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.vsuet.team.tutoringbackend.admin.service.AdminStatsService;
import ru.vsuet.team.tutoringbackend.admin.dto.AdminStats;

@RestController
@RequestMapping("/api/admin/stats")
@RequiredArgsConstructor
public class AdminStatsController {
    private final AdminStatsService stats;

    @GetMapping
    public AdminStats get() { return stats.stats(); }
}
