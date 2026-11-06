package ru.vsuet.team.tutoringbackend.tutor.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ru.vsuet.team.tutoringbackend.auth.AuthUser;
import ru.vsuet.team.tutoringbackend.catalog.dto.OfferResponse;
import ru.vsuet.team.tutoringbackend.catalog.dto.PriceRequest;
import ru.vsuet.team.tutoringbackend.catalog.dto.TutorCardResponse;
import ru.vsuet.team.tutoringbackend.catalog.dto.UpdateTutorProfileRequest;
import ru.vsuet.team.tutoringbackend.tutor.service.TutorProfileService;

import java.util.List;

@RestController
@RequestMapping("/api/tutor")
@RequiredArgsConstructor
public class TutorProfileController {
    private final TutorProfileService profile;

    @GetMapping("/profile")
    public TutorCardResponse get(@AuthenticationPrincipal AuthUser me) { return profile.profile(me.id()); }

    @PutMapping("/profile")
    public TutorCardResponse update(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody UpdateTutorProfileRequest r) {
        return profile.updateProfile(me.id(), r);
    }

    // «Управление услугами»: предметы и цены
    @GetMapping("/subjects")
    public List<OfferResponse> offers(@AuthenticationPrincipal AuthUser me) { return profile.listOffers(me.id()); }

    @PutMapping("/subjects/{subjectId}")      // добавить предмет или изменить цену
    public OfferResponse upsert(@AuthenticationPrincipal AuthUser me, @PathVariable Long subjectId,
                                @Valid @RequestBody PriceRequest r) {
        return profile.upsertOffer(me.id(), subjectId, r.pricePerHour());
    }

    @DeleteMapping("/subjects/{subjectId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AuthUser me, @PathVariable Long subjectId) {
        profile.deleteOffer(me.id(), subjectId);
    }
}
