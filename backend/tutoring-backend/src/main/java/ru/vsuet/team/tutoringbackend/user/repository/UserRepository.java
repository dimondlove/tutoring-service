package ru.vsuet.team.tutoringbackend.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.vsuet.team.tutoringbackend.user.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
