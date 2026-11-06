package ru.vsuet.team.tutoringbackend.booking;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.vsuet.team.tutoringbackend.catalog.entity.Subject;
import ru.vsuet.team.tutoringbackend.catalog.entity.Tutor;
import ru.vsuet.team.tutoringbackend.support.PostgresTestBase;
import ru.vsuet.team.tutoringbackend.user.Role;
import ru.vsuet.team.tutoringbackend.user.User;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class BookingApiIT extends PostgresTestBase {
    @Autowired
    MockMvc mvc;

    Subject math;
    Tutor tutor;
    User s1, s2;

    @BeforeEach
    void setUp() {
        math = fx.subject("Math");
        tutor = fx.tutor("t1@t.com", math);
        s1 = fx.user("s1@t.com", Role.STUDENT);
        s2 = fx.user("s2@t.com", Role.STUDENT);
    }

    private ResultActions book(User student, Tutor t, Instant start, int minutes, boolean intro) throws Exception {
        String body = """
                {"tutorId":%d,"subjectId":%d,"startTime":"%s","durationMinutes":%d,"intro":%b}"""
                .formatted(t.getId(), math.getId(), start, minutes, intro);
        return mvc.perform(post("/api/bookings").header(AUTH, bearer(student))
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions book(User student, Tutor t, Instant start) throws Exception {
        return book(student, t, start, 60, false);
    }

    private long idOf(ResultActions r) throws Exception {
        return ((Number) JsonPath.read(r.andReturn().getResponse().getContentAsString(), "$.id")).longValue();
    }

    @Test
    void createsPendingBookingWithPrice() throws Exception {
        book(s1, tutor, fx.slot(3, 10, 0)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.price").value(1000.0));
    }

    @Test void priceDependsOnDuration() throws Exception {
        book(s1, tutor, fx.slot(3, 10, 0), 90, false).andExpect(status().isCreated())
                .andExpect(jsonPath("$.price").value(1500.0));
    }

    @Test void notAllowedDurationIsRejected() throws Exception {
        book(s1, tutor, fx.slot(3, 10, 0), 50, false).andExpect(status().isBadRequest());
    }

    @Test void sameSlotIsRejected() throws Exception {
        book(s1, tutor, fx.slot(3, 10, 0)).andExpect(status().isCreated());
        book(s2, tutor, fx.slot(3, 10, 0)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SLOT_TAKEN"));
    }

    @Test void overlappingWithShiftedStartIsRejected() throws Exception {
        book(s1, tutor, fx.slot(3, 10, 0)).andExpect(status().isCreated());   // 10:00–11:00
        book(s2, tutor, fx.slot(3, 10, 30)).andExpect(status().isConflict()); // 10:30–11:30
    }

    @Test void adjacentBookingsAreAllowed() throws Exception {
        book(s1, tutor, fx.slot(3, 10, 0)).andExpect(status().isCreated());
        book(s2, tutor, fx.slot(3, 11, 0)).andExpect(status().isCreated());
    }

    @Test void differentTutorsSameTimeAreAllowed() throws Exception {
        Tutor other = fx.tutor("t2@t.com", math);
        book(s1, tutor, fx.slot(3, 10, 0)).andExpect(status().isCreated());
        book(s2, other, fx.slot(3, 10, 0)).andExpect(status().isCreated());
    }

    @Test void studentCannotBeAtTwoLessonsAtOnce() throws Exception {
        Tutor other = fx.tutor("t2@t.com", math);
        book(s1, tutor, fx.slot(3, 10, 0)).andExpect(status().isCreated());
        book(s1, other, fx.slot(3, 10, 0)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("STUDENT_BUSY"));
    }

    @Test void outsideWorkingHoursIsRejected() throws Exception {
        book(s1, tutor, fx.slot(3, 20, 0)).andExpect(status().isBadRequest());
    }

    @Test void startOffTheGridIsRejected() throws Exception {
        book(s1, tutor, fx.slot(3, 10, 7)).andExpect(status().isBadRequest());
    }

    @Test void cancelledSlotCanBeBookedAgain() throws Exception {
        long id = idOf(book(s1, tutor, fx.slot(3, 10, 0)));
        mvc.perform(post("/api/bookings/" + id + "/cancel").header(AUTH, bearer(s1))).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        book(s2, tutor, fx.slot(3, 10, 0)).andExpect(status().isCreated());
    }

    @Test void declinedBookingFreesTheSlot() throws Exception {
        long id = idOf(book(s1, tutor, fx.slot(3, 10, 0)));
        mvc.perform(post("/api/tutor/bookings/" + id + "/decline").header(AUTH, bearer(tutor.getUser())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("REJECTED"));
        book(s2, tutor, fx.slot(3, 10, 0)).andExpect(status().isCreated());
    }

    @Test void introLessonIsFreeAndOnlyOncePerTutor() throws Exception {
        book(s1, tutor, fx.slot(3, 10, 0), 30, true).andExpect(status().isCreated())
                .andExpect(jsonPath("$.price").value(0.0));
        book(s1, tutor, fx.slot(3, 12, 0), 30, true).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INTRO_ALREADY_USED"));
    }

    @Test void slotsEndpointReflectsBooking() throws Exception {
        LocalDate date = LocalDate.now(ZoneId.of("Europe/Moscow")).plusDays(3);
        book(s1, tutor, fx.slot(3, 10, 0)).andExpect(status().isCreated());
        mvc.perform(get("/api/tutors/{id}/slots", tutor.getId())
                        .param("subjectId", math.getId().toString())
                        .param("durationMinutes", "60").param("date", date.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.start == '" + fx.slot(3, 10, 0) + "')]").isEmpty());
    }

    @Test void catalogFiltersByPrice() throws Exception {
        mvc.perform(get("/api/tutors").param("maxPrice", "500")).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/tutors").param("subjectId", math.getId().toString()).param("maxPrice", "1500"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
    }
}
