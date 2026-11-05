package ru.vsuet.team.tutoringbackend.booking;

import java.util.EnumSet;
import java.util.Set;

public enum BookingStatus {
    PENDING, CONFIRMED, REJECTED, CANCELLED, COMPLETED;

    /** Статусы, занимающие слот. */
    public static final Set<BookingStatus> BLOCKING = EnumSet.of(PENDING, CONFIRMED, COMPLETED);
    /** Ещё не состоявшиеся: вкладка «Предстоящие». */
    public static final Set<BookingStatus> OPEN = EnumSet.of(PENDING, CONFIRMED);
}