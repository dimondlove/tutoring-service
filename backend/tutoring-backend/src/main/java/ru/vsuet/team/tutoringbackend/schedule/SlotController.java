package ru.vsuet.team.tutoringbackend.schedule;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.vsuet.team.tutoringbackend.schedule.dto.SlotResponse;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class SlotController {
    private final SlotService slots;

    @GetMapping("/api/tutors/{id}/slots")
    public ResponseEntity<List<SlotResponse>> slots(
            @PathVariable Long id,
            @RequestParam Long subjectId,
            @RequestParam int durationMinutes,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(slots.findSlots(id, subjectId, durationMinutes, date));
    }
}
