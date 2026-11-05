package ru.vsuet.team.tutoringbackend.catalog.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.vsuet.team.tutoringbackend.catalog.entity.TutorSubject;

import java.util.List;
import java.util.Optional;

public interface TutorSubjectRepository extends JpaRepository<TutorSubject, Long> {
    @EntityGraph(attributePaths = "subject")
    List<TutorSubject> findByTutor_IdOrderBySubject_Name(Long tutorId);

    @EntityGraph(attributePaths = "subject")
    Optional<TutorSubject> findByTutor_IdAndSubject_Id(Long tutorId, Long subjectId);

    boolean existsByTutor_IdAndSubject_Id(Long tutorId, Long subjectId);

    void deleteByTutor_IdAndSubject_Id(Long tutorId, Long subjectId);
}
