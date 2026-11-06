package ru.vsuet.team.tutoringbackend.booking;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class BookingMaintenanceJob {
    private final BookingService bookings;

    @Scheduled(initialDelayString = "PT10S", fixedDelayString = "PT1M")
    public void run() {
        int n = bookings.finalizeBookings();
        if (n > 0) log.info("Booking statuses updated: {}", n);
    }
}
