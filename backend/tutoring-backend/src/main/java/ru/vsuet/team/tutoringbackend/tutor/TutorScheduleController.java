package ru.vsuet.team.tutoringbackend.tutor;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ru.vsuet.team.tutoringbackend.auth.AuthUser;
import ru.vsuet.team.tutoringbackend.schedule.ScheduleManagementService;
import ru.vsuet.team.tutoringbackend.schedule.dto.TimeOffRequest;
import ru.vsuet.team.tutoringbackend.schedule.dto.TimeOffResponse;
import ru.vsuet.team.tutoringbackend.schedule.dto.UpdateWorkingHoursRequest;
import ru.vsuet.team.tutoringbackend.schedule.dto.WorkingHoursDto;

import java.util.List;

@RestController
@RequestMapping("/api/tutor")
@RequiredArgsConstructor
public class TutorScheduleController {
    private final ScheduleManagementService schedule;
    private final TutorProfileService profile;

    @GetMapping("/working-hours")
    public List<WorkingHoursDto> workingHours(@AuthenticationPrincipal AuthUser me) {
        return schedule.getWorkingHours(profile.tutorIdOf(me.id()));
    }

    @PutMapping("/working-hours")
    public List<WorkingHoursDto> setWorkingHours(@AuthenticationPrincipal AuthUser me,
                                                 @Valid @RequestBody UpdateWorkingHoursRequest r) {
        return schedule.replaceWorkingHours(profile.tutorIdOf(me.id()), r.items());
    }

    @GetMapping("/time-off")
    public List<TimeOffResponse> timeOff(@AuthenticationPrincipal AuthUser me) {
        return schedule.listTimeOff(profile.tutorIdOf(me.id()));
    }

    @PostMapping("/time-off")
    @ResponseStatus(HttpStatus.CREATED)
    public TimeOffResponse addTimeOff(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody TimeOffRequest r) {
        return schedule.addTimeOff(profile.tutorIdOf(me.id()), r);
    }

    @DeleteMapping("/time-off/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTimeOff(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        schedule.deleteTimeOff(profile.tutorIdOf(me.id()), id);
    }
}
