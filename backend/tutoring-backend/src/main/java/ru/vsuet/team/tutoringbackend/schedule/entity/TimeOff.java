package ru.vsuet.team.tutoringbackend.schedule.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "time_off")
@Getter
@Setter
@NoArgsConstructor
public class TimeOff {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long tutorId;
    private Instant startAt;
    private Instant endAt;
    private String reason;
}
