package ru.vsuet.team.tutoringbackend.schedule.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;

@Entity
@Table(name = "working_hours")
@Getter
@Setter
@NoArgsConstructor
public class WorkingHours {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long tutorId;
    private int dayOfWeek;        // 1 = понедельник … 7 = воскресенье
    private LocalTime startTime;  // локальное время в app.timezone
    private LocalTime endTime;
}
