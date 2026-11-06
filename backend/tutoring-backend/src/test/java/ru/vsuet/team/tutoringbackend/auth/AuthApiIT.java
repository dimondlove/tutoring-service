package ru.vsuet.team.tutoringbackend.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import ru.vsuet.team.tutoringbackend.support.PostgresTestBase;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthApiIT extends PostgresTestBase {
    @Autowired
    MockMvc mvc;

    private ResultActions post(String url, String json) throws Exception {
        return mvc.perform(MockMvcRequestBuilders.post(url).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private static final String REGISTER =
            "{\"email\":\"new@t.com\",\"password\":\"Password1!\",\"fullName\":\"New User\"}";

    @Test
    void registersStudentAndReturnsToken() throws Exception {
        post("/api/auth/register", REGISTER).andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.role").value("STUDENT"));
    }

    @Test
    void duplicateEmailIsRejected() throws Exception {
        post("/api/auth/register", REGISTER).andExpect(status().isCreated());
        post("/api/auth/register", REGISTER).andExpect(status().isConflict());
    }

    @Test
    void wrongPasswordGives401() throws Exception {
        post("/api/auth/register", REGISTER).andExpect(status().isCreated());
        post("/api/auth/login", "{\"email\":\"new@t.com\",\"password\":\"wrong\"}").andExpect(status().isUnauthorized());
    }

    @Test
    void shortPasswordGives400() throws Exception {
        post("/api/auth/register", "{\"email\":\"a@t.com\",\"password\":\"123\",\"fullName\":\"A\"}")
                .andExpect(status().isBadRequest());
    }
}
