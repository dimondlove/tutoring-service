package ru.vsuet.team.tutoringbackend.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.vsuet.team.tutoringbackend.booking.dto.BookingResponse;
import ru.vsuet.team.tutoringbackend.booking.dto.CreateBookingRequest;
import ru.vsuet.team.tutoringbackend.booking.service.BookingService;
import ru.vsuet.team.tutoringbackend.catalog.entity.Subject;
import ru.vsuet.team.tutoringbackend.catalog.entity.Tutor;
import ru.vsuet.team.tutoringbackend.support.PostgresTestBase;
import ru.vsuet.team.tutoringbackend.user.Role;
import ru.vsuet.team.tutoringbackend.user.User;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


class AccessControlIT extends PostgresTestBase {
    @Autowired
    MockMvc mvc;
    @Autowired
    BookingService bookingService;

    private BookingResponse book(User student, Tutor tutor, Subject subject, int hour) {
        return bookingService.create(student.getId(),
                new CreateBookingRequest(tutor.getId(), subject.getId(), fx.slot(3, hour, 0), 60, false));
    }

    @Test
    void protectedEndpointsRequireToken() throws Exception {
        mvc.perform(get("/api/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/bookings/my/upcoming")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/tutor/requests")).andExpect(status().isUnauthorized());
    }

    @Test
    void catalogIsPublic() throws Exception {
        mvc.perform(get("/api/subjects")).andExpect(status().isOk());
        mvc.perform(get("/api/tutors")).andExpect(status().isOk());
    }

    @Test
    void studentCannotUseAdminOrTutorEndpoints() throws Exception {
        User s = fx.user("s@t.com", Role.STUDENT);
        mvc.perform(get("/api/admin/users").header(AUTH, bearer(s))).andExpect(status().isForbidden());
        mvc.perform(get("/api/tutor/requests").header(AUTH, bearer(s))).andExpect(status().isForbidden());
    }

    @Test
    void adminCanUseAdminEndpoints() throws Exception {
        User a = fx.user("a@t.com", Role.ADMIN);
        mvc.perform(get("/api/admin/users").header(AUTH, bearer(a))).andExpect(status().isOk());
        mvc.perform(get("/api/admin/stats").header(AUTH, bearer(a))).andExpect(status().isOk());
    }

    @Test
    void tutorCannotCreateBooking() throws Exception {
        var math = fx.subject("Math");
        var tutor = fx.tutor("t@t.com", math);
        String body = """
                {"tutorId":%d,"subjectId":%d,"startTime":"%s","durationMinutes":60,"intro":false}"""
                .formatted(tutor.getId(), math.getId(), fx.slot(3, 10, 0));
        mvc.perform(post("/api/bookings").header(AUTH, bearer(tutor.getUser()))
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
    }

    @Test void studentCannotCancelForeignBooking() throws Exception {
        var math = fx.subject("Math");
        var tutor = fx.tutor("t@t.com", math);
        User owner = fx.user("s1@t.com", Role.STUDENT);
        User stranger = fx.user("s2@t.com", Role.STUDENT);
        var booking = book(owner, tutor, math, 10);
        mvc.perform(post("/api/bookings/" + booking.id() + "/cancel").header(AUTH, bearer(stranger)))
                .andExpect(status().isForbidden());
    }

    @Test void tutorCannotAcceptForeignBooking() throws Exception {
        var math = fx.subject("Math");
        var tutor = fx.tutor("t1@t.com", math);
        var otherTutor = fx.tutor("t2@t.com", math);
        User student = fx.user("s@t.com", Role.STUDENT);
        var booking = book(student, tutor, math, 10);
        mvc.perform(post("/api/tutor/bookings/" + booking.id() + "/accept").header(AUTH, bearer(otherTutor.getUser())))
                .andExpect(status().isForbidden());
    }

    @Test void myBookingsContainOnlyOwn() throws Exception {
        var math = fx.subject("Math");
        var tutor = fx.tutor("t@t.com", math);
        User s1 = fx.user("s1@t.com", Role.STUDENT);
        User s2 = fx.user("s2@t.com", Role.STUDENT);
        book(s1, tutor, math, 10);
        book(s2, tutor, math, 12);
        mvc.perform(get("/api/bookings/my/upcoming").header(AUTH, bearer(s1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].student.id").value(s1.getId()));
    }

    @Test void blockedUserTokenStopsWorking() throws Exception {
        User s = fx.user("s@t.com", Role.STUDENT);
        String token = bearer(s);
        mvc.perform(get("/api/me").header(AUTH, token)).andExpect(status().isOk());
        jdbc.update("update users set enabled = false where id = ?", s.getId());
        mvc.perform(get("/api/me").header(AUTH, token)).andExpect(status().isUnauthorized());
    }
}
