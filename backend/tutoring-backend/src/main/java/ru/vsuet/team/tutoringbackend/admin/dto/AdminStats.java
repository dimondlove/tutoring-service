package ru.vsuet.team.tutoringbackend.admin.dto;

import java.util.List;
import java.util.Map;

public record AdminStats(Map<String, Long> usersByRole, Map<String, Long> bookingsByStatus,
                         long newUsersLast7Days, long upcomingBookingsNext7Days, double cancellationRatePercent,
                         List<TopTutor> topTutors, List<TopSubject> topSubjects) {

    public record TopTutor(Long tutorId, String fullName, long completedLessons, double rating) {}

    public record TopSubject(String name, long bookings) {}

}
