package ru.vsuet.team.tutoringbackend.tutor;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.vsuet.team.tutoringbackend.catalog.dto.OfferResponse;
import ru.vsuet.team.tutoringbackend.catalog.dto.TutorCardResponse;
import ru.vsuet.team.tutoringbackend.catalog.dto.UpdateTutorProfileRequest;
import ru.vsuet.team.tutoringbackend.catalog.entity.Subject;
import ru.vsuet.team.tutoringbackend.catalog.entity.Tutor;
import ru.vsuet.team.tutoringbackend.catalog.entity.TutorSubject;
import ru.vsuet.team.tutoringbackend.catalog.repository.SubjectRepository;
import ru.vsuet.team.tutoringbackend.catalog.repository.TutorRepository;
import ru.vsuet.team.tutoringbackend.catalog.repository.TutorSubjectRepository;
import ru.vsuet.team.tutoringbackend.catalog.service.RatingService;
import ru.vsuet.team.tutoringbackend.common.NotFoundException;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TutorProfileService {
    private final TutorRepository tutors;
    private final TutorSubjectRepository offers;
    private final SubjectRepository subjects;
    private final RatingService ratings;

    @Transactional(readOnly = true)
    public Long tutorIdOf(Long userId) {
        return tutors.findByUserId(userId).map(Tutor::getId)
                .orElseThrow(() -> new NotFoundException("TUTOR_PROFILE_NOT_FOUND", "Профиль репетитора не найден"));
    }

    @Transactional(readOnly = true)
    public TutorCardResponse profile(Long userId) { return card(tutorIdOf(userId)); }

    @Transactional
    public TutorCardResponse updateProfile(Long userId, UpdateTutorProfileRequest r) {
        Tutor t = tutors.findById(tutorIdOf(userId)).orElseThrow();
        t.setBio(r.bio());
        t.setPhotoUrl(r.photoUrl());
        t.setExperienceYears(r.experienceYears());
        return card(t.getId());
    }

    @Transactional(readOnly = true)
    public List<OfferResponse> listOffers(Long userId) {
        return OfferResponse.listOf(offers.findByTutor_IdOrderBySubject_Name(tutorIdOf(userId)), false);
    }

    /** Добавление или изменение цены по предмету. */
    @Transactional
    public OfferResponse upsertOffer(Long userId, Long subjectId, BigDecimal price) {
        Long tutorId = tutorIdOf(userId);
        Subject subject = subjects.findById(subjectId).filter(Subject::isActive)
                .orElseThrow(() -> new NotFoundException("SUBJECT_NOT_FOUND", "Предмет не найден"));
        TutorSubject o = offers.findByTutor_IdAndSubject_Id(tutorId, subjectId).orElseGet(() -> {
            TutorSubject n = new TutorSubject();
            n.setTutor(tutors.getReferenceById(tutorId));
            n.setSubject(subject);
            return n;
        });
        o.setPricePerHour(price);
        return OfferResponse.from(offers.save(o));
    }

    @Transactional
    public void deleteOffer(Long userId, Long subjectId) {
        offers.deleteByTutor_IdAndSubject_Id(tutorIdOf(userId), subjectId);   // старые записи не затрагиваются
    }

    private TutorCardResponse card(Long tutorId) {
        Tutor t = tutors.findWithDetailsById(tutorId).orElseThrow();
        var s = ratings.tutorStats(List.of(tutorId)).getOrDefault(tutorId, RatingService.Stats.NONE);
        return TutorCardResponse.of(t, OfferResponse.listOf(t.getOffers(), false), s);
    }
}
