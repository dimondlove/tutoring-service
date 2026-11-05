package ru.vsuet.team.tutoringbackend.catalog;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.vsuet.team.tutoringbackend.catalog.dto.SubjectResponse;
import ru.vsuet.team.tutoringbackend.catalog.dto.TutorCardResponse;
import ru.vsuet.team.tutoringbackend.catalog.service.CatalogService;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class CatalogController {
    private final CatalogService catalog;

    @GetMapping("/api/subjects")
    public List<SubjectResponse> subjects() { return catalog.activeSubjects(); }

    @GetMapping("/api/tutors")
    public List<TutorCardResponse> tutors(
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Double minRating,
            @RequestParam(required = false) Integer minExperience,
            @RequestParam(required = false) String sort) {      // popular | price | rating | experience
        return catalog.search(subjectId, q, minPrice, maxPrice, minRating, minExperience, sort);
    }

    @GetMapping("/api/tutors/{id}")
    public TutorCardResponse tutor(@PathVariable Long id) { return catalog.tutor(id); }
}
