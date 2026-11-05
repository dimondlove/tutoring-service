package ru.vsuet.team.tutoringbackend.user;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String email;
    private String passwordHash;
    private String fullName;
    private String phone;
    private LocalDate birthDate;
    @Enumerated(EnumType.STRING)
    private Role role;
    private boolean enabled = true;
    private Instant createdAt = Instant.now();
}
