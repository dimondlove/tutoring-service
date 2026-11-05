package ru.vsuet.team.tutoringbackend.catalog.dto;

import ru.vsuet.team.tutoringbackend.catalog.entity.Tutor;
import ru.vsuet.team.tutoringbackend.catalog.service.RatingService;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

public record TutorCardResponse(Long id, Long userId, String fullName, String photoUrl, String bio,
                                int experienceYears, double rating, long ratingCount, boolean active,
                                BigDecimal minPricePerHour, List<OfferResponse> offers) {

    public static TutorCardResponse of(Tutor tutor, List<OfferResponse> offers, RatingService.Stats stats) {
        BigDecimal min = offers.stream().map(OfferResponse::pricePerHour).min(Comparator.naturalOrder()).orElse(null);
        return new TutorCardResponse(tutor.getId(), tutor.getUser().getId(), tutor.getUser().getFullName(),
                tutor.getPhotoUrl(), tutor.getBio(), tutor.getExperienceYears(), stats.average(), stats.count(),
                tutor.isActive(), min, offers);
    }
}
