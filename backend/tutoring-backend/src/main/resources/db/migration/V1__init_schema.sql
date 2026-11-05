CREATE TABLE users (
                       id            BIGSERIAL PRIMARY KEY,
                       email         VARCHAR(255) NOT NULL UNIQUE,
                       password_hash VARCHAR(100) NOT NULL,
                       full_name     VARCHAR(255) NOT NULL,
                       phone         VARCHAR(32),
                       birth_date    DATE,
                       role          VARCHAR(20)  NOT NULL CHECK (role IN ('STUDENT','TUTOR','ADMIN')),
                       enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
                       created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE subject (
                         id     BIGSERIAL PRIMARY KEY,
                         name   VARCHAR(100) NOT NULL UNIQUE,
                         active BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE tutor (
                       id               BIGSERIAL PRIMARY KEY,
                       user_id          BIGINT  NOT NULL UNIQUE REFERENCES users(id),
                       bio              TEXT,
                       photo_url        VARCHAR(500),
                       experience_years INT     NOT NULL DEFAULT 0 CHECK (experience_years >= 0),
                       active           BOOLEAN NOT NULL DEFAULT TRUE
);

-- предмет, который ведёт репетитор, и его цена за час
CREATE TABLE tutor_subject (
                               id             BIGSERIAL PRIMARY KEY,
                               tutor_id       BIGINT        NOT NULL REFERENCES tutor(id),
                               subject_id     BIGINT        NOT NULL REFERENCES subject(id),
                               price_per_hour NUMERIC(10,2) NOT NULL CHECK (price_per_hour >= 0),
                               UNIQUE (tutor_id, subject_id)
);

CREATE TABLE working_hours (
                               id          BIGSERIAL PRIMARY KEY,
                               tutor_id    BIGINT NOT NULL REFERENCES tutor(id),
                               day_of_week INT    NOT NULL CHECK (day_of_week BETWEEN 1 AND 7),   -- 1 = понедельник
                               start_time  TIME   NOT NULL,
                               end_time    TIME   NOT NULL,
                               CHECK (end_time > start_time)
);

CREATE TABLE time_off (
                          id       BIGSERIAL PRIMARY KEY,
                          tutor_id BIGINT      NOT NULL REFERENCES tutor(id),
                          start_at TIMESTAMPTZ NOT NULL,
                          end_at   TIMESTAMPTZ NOT NULL,
                          reason   VARCHAR(255),
                          CHECK (end_at > start_at)
);

CREATE TABLE booking (
                         id             BIGSERIAL PRIMARY KEY,
                         student_id     BIGINT        NOT NULL REFERENCES users(id),
                         tutor_id       BIGINT        NOT NULL REFERENCES tutor(id),
                         subject_id     BIGINT        NOT NULL REFERENCES subject(id),
                         start_time     TIMESTAMPTZ   NOT NULL,
                         end_time       TIMESTAMPTZ   NOT NULL,
                         status         VARCHAR(20)   NOT NULL DEFAULT 'PENDING'
                             CHECK (status IN ('PENDING','CONFIRMED','REJECTED','CANCELLED','COMPLETED')),
                         intro          BOOLEAN       NOT NULL DEFAULT FALSE,
                         price          NUMERIC(10,2) NOT NULL CHECK (price >= 0),
                         homework       TEXT,
                         tutor_rating   INT CHECK (tutor_rating   BETWEEN 1 AND 5),   -- оценка репетитора учеником
                         student_rating INT CHECK (student_rating BETWEEN 1 AND 5),   -- оценка ученика репетитором
                         cancelled_at   TIMESTAMPTZ,
                         created_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
                         CHECK (end_time > start_time)
);