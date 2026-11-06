package ru.vsuet.team.tutoringbackend.booking;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import ru.vsuet.team.tutoringbackend.booking.dto.CreateBookingRequest;
import ru.vsuet.team.tutoringbackend.booking.service.BookingService;
import ru.vsuet.team.tutoringbackend.catalog.entity.Subject;
import ru.vsuet.team.tutoringbackend.catalog.entity.Tutor;
import ru.vsuet.team.tutoringbackend.support.PostgresTestBase;
import ru.vsuet.team.tutoringbackend.user.Role;
import ru.vsuet.team.tutoringbackend.user.User;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


class BookingLifecycleIT extends PostgresTestBase {
    @Autowired
    MockMvc mvc;
    @Autowired
    BookingService bookingService;

    @Test
    void fullLifecycle() throws Exception {
        Subject math = fx.subject("Math");
        Tutor tutor = fx.tutor("t@t.com", math);
        User student = fx.user("s@t.com", Role.STUDENT);
        String tutorAuth = bearer(tutor.getUser());
        String studentAuth = bearer(student);

        long id = bookingService.create(student.getId(),
                new CreateBookingRequest(tutor.getId(), math.getId(), fx.slot(3, 10, 0), 60, false)).id();

        mvc.perform(get("/api/tutor/requests").header(AUTH, tutorAuth))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(post("/api/tutor/bookings/" + id + "/accept").header(AUTH, tutorAuth))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CONFIRMED"));
        mvc.perform(put("/api/tutor/bookings/" + id + "/homework").header(AUTH, tutorAuth)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"homework\":\"Tasks 1-10\"}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/bookings/my/upcoming").header(AUTH, studentAuth))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].homework").value("Tasks 1-10"));

        // оценить до проведения нельзя
        mvc.perform(post("/api/bookings/" + id + "/rate").header(AUTH, studentAuth)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"rating\":5}"))
                .andExpect(status().isConflict());

        // «занятие прошло»
        jdbc.update("update booking set start_time = now() - interval '2 hours', end_time = now() - interval '1 hour' where id = ?", id);
        bookingService.finalizeBookings();

        mvc.perform(get("/api/bookings/my/past").header(AUTH, studentAuth))
                .andExpect(jsonPath("$[0].status").value("COMPLETED"));
        mvc.perform(post("/api/bookings/" + id + "/rate").header(AUTH, studentAuth)
                .contentType(MediaType.APPLICATION_JSON).content("{\"rating\":5}")).andExpect(status().isOk());
        mvc.perform(post("/api/tutor/bookings/" + id + "/rate-student").header(AUTH, tutorAuth)
                .contentType(MediaType.APPLICATION_JSON).content("{\"rating\":4}")).andExpect(status().isOk());
        mvc.perform(get("/api/tutors/" + tutor.getId()))
                .andExpect(jsonPath("$.rating").value(5.0)).andExpect(jsonPath("$.ratingCount").value(1));
    }

    @Test
    void unansweredPendingExpiresWhenStartPasses() throws Exception {
        Subject math = fx.subject("Math");
        Tutor tutor = fx.tutor("t@t.com", math);
        User student = fx.user("s@t.com", Role.STUDENT);
        long id = bookingService.create(student.getId(),
                new CreateBookingRequest(tutor.getId(), math.getId(), fx.slot(3, 10, 0), 60, false)).id();
        jdbc.update("update booking set start_time = now() - interval '2 hours', end_time = now() - interval '1 hour' where id = ?", id);
        bookingService.finalizeBookings();
        assertThat(jdbc.queryForObject("select status from booking where id = ?", String.class, id)).isEqualTo("REJECTED");
    }
}
