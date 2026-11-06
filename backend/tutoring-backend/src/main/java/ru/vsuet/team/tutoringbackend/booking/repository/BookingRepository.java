package ru.vsuet.team.tutoringbackend.booking.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.vsuet.team.tutoringbackend.booking.Booking;
import ru.vsuet.team.tutoringbackend.booking.BookingStatus;
import ru.vsuet.team.tutoringbackend.user.User;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    // ---- проверки пересечений ----
    @Query("""
        select count(b) > 0 from Booking b
        where b.tutor.id = :tutorId and b.status in :statuses
          and b.startTime < :rangeEnd and b.endTime > :rangeStart""")
    boolean existsForTutor(@Param("tutorId") Long tutorId, @Param("rangeStart") Instant rangeStart,
                           @Param("rangeEnd") Instant rangeEnd, @Param("statuses") Collection<BookingStatus> statuses);

    default boolean existsActiveForTutor(Long tutorId, Instant start, Instant end) {
        return existsForTutor(tutorId, start, end, BookingStatus.BLOCKING);
    }

    @Query("""
        select count(b) > 0 from Booking b
        where b.student.id = :studentId and b.status in :statuses
          and b.startTime < :rangeEnd and b.endTime > :rangeStart""")
    boolean existsForStudent(@Param("studentId") Long studentId, @Param("rangeStart") Instant rangeStart,
                             @Param("rangeEnd") Instant rangeEnd, @Param("statuses") Collection<BookingStatus> statuses);

    default boolean existsActiveForStudent(Long studentId, Instant start, Instant end) {
        return existsForStudent(studentId, start, end, BookingStatus.BLOCKING);
    }

    @Query("""
        select count(b) > 0 from Booking b
        where b.student.id = :studentId and b.tutor.id = :tutorId and b.intro = true
          and b.status in :statuses""")
    boolean existsIntro(@Param("studentId") Long studentId, @Param("tutorId") Long tutorId,
                        @Param("statuses") Collection<BookingStatus> statuses);

    // ---- выборки ----
    @EntityGraph(attributePaths = {"student", "tutor", "tutor.user", "subject"})
    Optional<Booking> findWithDetailsById(Long id);

    @EntityGraph(attributePaths = {"student", "tutor", "tutor.user", "subject"})
    @Query("""
        select b from Booking b
        where b.tutor.id = :tutorId and b.status in :statuses
          and b.startTime < :rangeEnd and b.endTime > :rangeStart
        order by b.startTime""")
    List<Booking> findForTutorBetween(@Param("tutorId") Long tutorId, @Param("rangeStart") Instant rangeStart,
                                      @Param("rangeEnd") Instant rangeEnd,
                                      @Param("statuses") Collection<BookingStatus> statuses);

    @EntityGraph(attributePaths = {"student", "tutor", "tutor.user", "subject"})
    @Query("""
        select b from Booking b
        where b.status in :statuses and b.startTime < :rangeEnd and b.endTime > :rangeStart
        order by b.startTime""")
    List<Booking> findAllBetween(@Param("rangeStart") Instant rangeStart, @Param("rangeEnd") Instant rangeEnd,
                                 @Param("statuses") Collection<BookingStatus> statuses);

    @EntityGraph(attributePaths = {"student", "tutor", "tutor.user", "subject"})
    @Query("""
        select b from Booking b
        where b.student.id = :studentId and b.status in :open and b.endTime > :now
        order by b.startTime""")
    List<Booking> findUpcomingForStudent(@Param("studentId") Long studentId, @Param("now") Instant now,
                                         @Param("open") Collection<BookingStatus> open);

    @EntityGraph(attributePaths = {"student", "tutor", "tutor.user", "subject"})
    @Query("""
        select b from Booking b
        where b.student.id = :studentId and (b.status not in :open or b.endTime <= :now)
        order by b.startTime desc""")
    List<Booking> findPastForStudent(@Param("studentId") Long studentId, @Param("now") Instant now,
                                     @Param("open") Collection<BookingStatus> open);

    @EntityGraph(attributePaths = {"student", "tutor", "tutor.user", "subject"})
    @Query("""
        select b from Booking b
        where b.tutor.id = :tutorId and b.status = ru.vsuet.team.tutoringbackend.booking.BookingStatus.PENDING
          and b.endTime > :now
        order by b.startTime""")
    List<Booking> findPendingForTutor(@Param("tutorId") Long tutorId, @Param("now") Instant now);

    @Query("select distinct b.student from Booking b where b.tutor.id = :tutorId and b.status in :statuses")
    List<User> findStudentsOfTutor(@Param("tutorId") Long tutorId,
                                   @Param("statuses") Collection<BookingStatus> statuses);

    // ---- рейтинги ----
    @Query("""
        select b.tutor.id, avg(b.tutorRating), count(b.tutorRating) from Booking b
        where b.tutor.id in :ids and b.tutorRating is not null group by b.tutor.id""")
    List<Object[]> tutorRatingStats(@Param("ids") Collection<Long> ids);

    @Query("""
        select b.student.id, avg(b.studentRating), count(b.studentRating) from Booking b
        where b.student.id in :ids and b.studentRating is not null group by b.student.id""")
    List<Object[]> studentRatingStats(@Param("ids") Collection<Long> ids);

    // ---- плановые переходы статусов ----
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update Booking b set b.status = ru.vsuet.team.tutoringbackend.booking.BookingStatus.REJECTED
        where b.status = ru.vsuet.team.tutoringbackend.booking.BookingStatus.PENDING and b.startTime <= :now""")
    int expirePending(@Param("now") Instant now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update Booking b set b.status = ru.vsuet.team.tutoringbackend.booking.BookingStatus.COMPLETED
        where b.status = ru.vsuet.team.tutoringbackend.booking.BookingStatus.CONFIRMED and b.endTime <= :now""")
    int completeFinished(@Param("now") Instant now);
}
