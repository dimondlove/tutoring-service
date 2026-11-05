package ru.vsuet.team.tutoringbackend.catalog.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.vsuet.team.tutoringbackend.catalog.entity.Tutor;

import java.util.List;
import java.util.Optional;

public interface TutorRepository extends JpaRepository<Tutor, Long> {
    @EntityGraph(attributePaths = {"user", "offers", "offers.subject"})
    List<Tutor> findAllByActiveTrue();

    @Override @EntityGraph(attributePaths = {"user", "offers", "offers.subject"})
    List<Tutor> findAll();

    @EntityGraph(attributePaths = {"user", "offers", "offers.subject"})
    Optional<Tutor> findWithDetailsById(Long id);

    Optional<Tutor> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    boolean existsByIdAndActiveTrue(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Tutor t where t.id = :id")
    Optional<Tutor> findByIdForUpdate(@Param("id") Long id);
}