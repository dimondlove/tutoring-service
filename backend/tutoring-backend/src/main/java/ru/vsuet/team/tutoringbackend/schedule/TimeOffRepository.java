package ru.vsuet.team.tutoringbackend.schedule;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface TimeOffRepository extends JpaRepository<TimeOff, Long> {
    @Query("""
        select t from TimeOff t
        where t.tutorId = :tutorId and t.startAt < :rangeEnd and t.endAt > :rangeStart
        order by t.startAt""")
    List<TimeOff> findOverlapping(@Param("tutorId") Long tutorId,
                                  @Param("rangeStart") Instant rangeStart,
                                  @Param("rangeEnd") Instant rangeEnd);

    List<TimeOff> findByTutorIdAndEndAtAfterOrderByStartAt(Long tutorId, Instant after);
}
