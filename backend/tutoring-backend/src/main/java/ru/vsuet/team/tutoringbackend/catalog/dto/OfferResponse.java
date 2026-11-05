package ru.vsuet.team.tutoringbackend.catalog.dto;

import ru.vsuet.team.tutoringbackend.catalog.entity.TutorSubject;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public record OfferResponse(Long subjectId, String subjectName, BigDecimal pricePerHour) {
    public static OfferResponse from(TutorSubject tutorSubject) {
        return new OfferResponse(tutorSubject.getSubject().getId(), tutorSubject.getSubject().getName(),
                tutorSubject.getPricePerHour());
    }
    public static List<OfferResponse> listOf(Collection<TutorSubject> offers, boolean onlyActiveSubjects) {
        return offers.stream()
                .filter(o -> !onlyActiveSubjects || o.getSubject().isActive())
                .sorted(Comparator.comparing((TutorSubject o) -> o.getSubject().getName()))
                .map(OfferResponse::from).toList();
    }
}
