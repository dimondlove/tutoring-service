package ru.vsuet.team.tutoringbackend.catalog.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.vsuet.team.tutoringbackend.catalog.dto.OfferResponse;
import ru.vsuet.team.tutoringbackend.catalog.dto.SubjectResponse;
import ru.vsuet.team.tutoringbackend.catalog.dto.TutorCardResponse;
import ru.vsuet.team.tutoringbackend.catalog.entity.Tutor;
import ru.vsuet.team.tutoringbackend.catalog.repository.SubjectRepository;
import ru.vsuet.team.tutoringbackend.catalog.repository.TutorRepository;
import ru.vsuet.team.tutoringbackend.common.NotFoundException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CatalogService {
    private final SubjectRepository subjects;
    private final TutorRepository tutors;
    private final RatingService ratings;

    @Transactional(readOnly = true)
    public List<SubjectResponse> activeSubjects() {
        return subjects.findByActiveTrueOrderByName().stream().map(SubjectResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<TutorCardResponse> search(Long subjectId, String q, BigDecimal minPrice, BigDecimal maxPrice,
                                          Double minRating, Integer minExperience, String sort) {
        List<Tutor> all = tutors.findAllByActiveTrue();
        Map<Long, RatingService.Stats> stats = ratings.tutorStats(all.stream().map(Tutor::getId).toList());
        String query = q == null ? "" : q.trim().toLowerCase();

        List<TutorCardResponse> cards = new ArrayList<>();
        for (Tutor t : all) {
            var s = stats.getOrDefault(t.getId(), RatingService.Stats.NONE);
            if (minRating != null && s.average() < minRating) continue;
            if (minExperience != null && t.getExperienceYears() < minExperience) continue;
            // фильтры по предмету и цене применяются к предложениям: в карточке остаются подходящие предметы
            List<OfferResponse> offers = OfferResponse.listOf(t.getOffers(), true).stream()
                    .filter(o -> subjectId == null || o.subjectId().equals(subjectId))
                    .filter(o -> query.isEmpty() || o.subjectName().toLowerCase().contains(query))
                    .filter(o -> minPrice == null || o.pricePerHour().compareTo(minPrice) >= 0)
                    .filter(o -> maxPrice == null || o.pricePerHour().compareTo(maxPrice) <= 0)
                    .toList();
            if (!offers.isEmpty()) cards.add(TutorCardResponse.of(t, offers, s));
        }
        cards.sort(comparator(sort));
        return cards;
    }

    /** Полная карточка (все активные предметы) для страницы записи. */
    @Transactional(readOnly = true)
    public TutorCardResponse tutor(Long id) {
        Tutor t = tutors.findWithDetailsById(id).filter(Tutor::isActive)
                .orElseThrow(() -> new NotFoundException("TUTOR_NOT_FOUND", "Репетитор не найден"));
        var s = ratings.tutorStats(List.of(id)).getOrDefault(id, RatingService.Stats.NONE);
        return TutorCardResponse.of(t, OfferResponse.listOf(t.getOffers(), true), s);
    }

    private Comparator<TutorCardResponse> comparator(String sort) {
        Comparator<TutorCardResponse> byRating = Comparator.comparingDouble(TutorCardResponse::rating).reversed();
        return switch (sort == null ? "popular" : sort) {
            case "price" -> Comparator.comparing(TutorCardResponse::minPricePerHour);
            case "rating" -> byRating;
            case "experience" -> Comparator.comparingInt(TutorCardResponse::experienceYears).reversed();
            default -> byRating.thenComparing(Comparator.comparingLong(TutorCardResponse::ratingCount).reversed());
        };
    }
}
