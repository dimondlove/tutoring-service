package ru.vsuet.team.tutoringbackend.catalog.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.vsuet.team.tutoringbackend.catalog.entity.Subject;

import java.util.List;
import java.util.Optional;

public interface SubjectRepository extends JpaRepository<Subject, Long> {
    List<Subject> findByActiveTrueOrderByName();

    Optional<Subject> findByNameIgnoreCase(String name);
}
