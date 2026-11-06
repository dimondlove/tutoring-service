package ru.vsuet.team.tutoringbackend.booking;

import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.vsuet.team.tutoringbackend.booking.dto.CreateBookingRequest;
import ru.vsuet.team.tutoringbackend.booking.service.BookingService;
import ru.vsuet.team.tutoringbackend.catalog.entity.Subject;
import ru.vsuet.team.tutoringbackend.catalog.entity.Tutor;
import ru.vsuet.team.tutoringbackend.common.ConflictException;
import ru.vsuet.team.tutoringbackend.support.PostgresTestBase;
import ru.vsuet.team.tutoringbackend.user.Role;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class BookingConcurrencyIT extends PostgresTestBase {
    @Autowired
    BookingService bookingService;

    @Test
    void onlyOneOfManyConcurrentRequestsGetsTheSlot() throws Exception {
        int n = 8;
        Subject math = fx.subject("Math");
        Tutor tutor = fx.tutor("tutor@t.com", math);
        Instant start = fx.slot(3, 10, 0);
        List<Long> studentIds = IntStream.range(0, n)
                .mapToObj(i -> fx.user("s" + i + "@t.com", Role.STUDENT).getId()).toList();

        ExecutorService pool = Executors.newFixedThreadPool(n);
        CountDownLatch ready = new CountDownLatch(n);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();
        for (Long id : studentIds) {
            results.add(pool.submit(() -> {
                ready.countDown();
                go.await();                                   // все стартуют одновременно
                try {
                    bookingService.create(id, new CreateBookingRequest(tutor.getId(), math.getId(), start, 60, false));
                    return true;
                } catch (ConflictException e) {
                    return false;
                }
            }));
        }
        ready.await();
        go.countDown();

        long success = 0;
        for (Future<Boolean> f : results) if (f.get(30, TimeUnit.SECONDS)) success++;
        pool.shutdown();

        assertThat(success).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from booking", Integer.class)).isEqualTo(1);
    }
}
