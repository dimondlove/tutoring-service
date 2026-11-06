package ru.vsuet.team.tutoringbackend.schedule.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.vsuet.team.tutoringbackend.schedule.entity.WorkingHours;

import java.util.List;

public interface WorkingHoursRepository extends JpaRepository<WorkingHours, Long> {
    List<WorkingHours> findByTutorIdOrderByDayOfWeekAscStartTimeAsc(Long tutorId);
    List<WorkingHours> findByTutorIdAndDayOfWeekOrderByStartTime(Long tutorId, int dayOfWeek);

    @Modifying
    @Query("delete from WorkingHours w where w.tutorId = :tutorId")
    void deleteAllForTutor(@Param("tutorId") Long tutorId);
}
