package ru.vsuet.team.tutoringbackend.catalog.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.vsuet.team.tutoringbackend.user.User;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "tutor")
@Getter
@Setter
@NoArgsConstructor
public class Tutor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id")
    private User user;

    @Column(columnDefinition = "text")
    private String bio;
    private String photoUrl;
    private int experienceYears;
    private boolean active = true;

    @OneToMany(mappedBy = "tutor")
    private Set<TutorSubject> offers = new HashSet<>();
}
