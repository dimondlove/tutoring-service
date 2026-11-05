package ru.vsuet.team.tutoringbackend.booking;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.vsuet.team.tutoringbackend.catalog.entity.Subject;
import ru.vsuet.team.tutoringbackend.catalog.entity.Tutor;
import ru.vsuet.team.tutoringbackend.user.User;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "booking")
@Getter
@Setter
@NoArgsConstructor
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "student_id")
    private User student;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "tutor_id")
    private Tutor tutor;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "subject_id")
    private Subject subject;

    private Instant startTime;
    private Instant endTime;
    @Enumerated(EnumType.STRING)
    private BookingStatus status = BookingStatus.PENDING;
    private boolean intro;
    private BigDecimal price;
    @Column(columnDefinition = "text")
    private String homework;
    private Integer tutorRating;     // оценка репетитора учеником
    private Integer studentRating;   // оценка ученика репетитором
    private Instant cancelledAt;
    private Instant createdAt;
}
