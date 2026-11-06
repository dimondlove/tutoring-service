package ru.vsuet.team.tutoringbackend.support;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.vsuet.team.tutoringbackend.user.User;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class PostgresTestBase {

    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");
    static { POSTGRES.start(); }

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        r.add("spring.datasource.username", POSTGRES::getUsername);
        r.add("spring.datasource.password", POSTGRES::getPassword);
    }

    protected static final String AUTH = "Authorization";

    @Autowired
    protected JdbcTemplate jdbc;

    @Autowired
    protected TestFixtures fx;

    @BeforeEach
    void cleanDb() {
        jdbc.execute("TRUNCATE booking, time_off, working_hours, tutor_subject, tutor, subject, users RESTART IDENTITY CASCADE");
    }

    protected String bearer(User u) { return "Bearer " + fx.token(u); }
}
