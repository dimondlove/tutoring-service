package ru.vsuet.team.tutoringbackend.admin.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.vsuet.team.tutoringbackend.admin.dto.CreateTutorRequest;
import ru.vsuet.team.tutoringbackend.admin.dto.UpdateTutorRequest;
import ru.vsuet.team.tutoringbackend.catalog.dto.OfferResponse;
import ru.vsuet.team.tutoringbackend.catalog.dto.SubjectRequest;
import ru.vsuet.team.tutoringbackend.catalog.dto.SubjectResponse;
import ru.vsuet.team.tutoringbackend.catalog.dto.TutorCardResponse;
import ru.vsuet.team.tutoringbackend.catalog.entity.Subject;
import ru.vsuet.team.tutoringbackend.catalog.entity.Tutor;
import ru.vsuet.team.tutoringbackend.catalog.repository.SubjectRepository;
import ru.vsuet.team.tutoringbackend.catalog.repository.TutorRepository;
import ru.vsuet.team.tutoringbackend.catalog.service.RatingService;
import ru.vsuet.team.tutoringbackend.common.BadRequestException;
import ru.vsuet.team.tutoringbackend.common.ConflictException;
import ru.vsuet.team.tutoringbackend.common.NotFoundException;
import ru.vsuet.team.tutoringbackend.user.Role;
import ru.vsuet.team.tutoringbackend.user.User;
import ru.vsuet.team.tutoringbackend.user.repository.UserRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminCatalogService {
    private final SubjectRepository subjects;
    private final TutorRepository tutors;
    private final UserRepository users;
    private final RatingService ratings;

    // ---- предметы ----
    @Transactional(readOnly = true)
    public List<SubjectResponse> allSubjects() {
        return subjects.findAll(Sort.by("name")).stream().map(SubjectResponse::from).toList();
    }

    @Transactional
    public SubjectResponse createSubject(SubjectRequest r) {
        String name = r.name().trim();
        if (subjects.findByNameIgnoreCase(name).isPresent())
            throw new ConflictException("SUBJECT_EXISTS", "Такой предмет уже есть");
        Subject s = new Subject();
        s.setName(name);
        if (r.active() != null) s.setActive(r.active());
        return SubjectResponse.from(subjects.save(s));
    }

    @Transactional
    public SubjectResponse updateSubject(Long id, SubjectRequest r) {
        Subject s = subjects.findById(id).orElseThrow(() -> new NotFoundException("SUBJECT_NOT_FOUND", "Предмет не найден"));
        String name = r.name().trim();
        subjects.findByNameIgnoreCase(name).filter(x -> !x.getId().equals(id)).ifPresent(x -> {
            throw new ConflictException("SUBJECT_EXISTS", "Такой предмет уже есть");
        });
        s.setName(name);
        if (r.active() != null) s.setActive(r.active());
        return SubjectResponse.from(s);
    }

    @Transactional   // мягкое удаление: на предмет ссылаются записи
    public void deactivateSubject(Long id) {
        subjects.findById(id).orElseThrow(() -> new NotFoundException("SUBJECT_NOT_FOUND", "Предмет не найден"))
                .setActive(false);
    }

    // ---- репетиторы ----
    @Transactional(readOnly = true)
    public List<TutorCardResponse> allTutors() {
        List<Tutor> all = tutors.findAll();
        var stats = ratings.tutorStats(all.stream().map(Tutor::getId).toList());
        return all.stream().map(t -> TutorCardResponse.of(t, OfferResponse.listOf(t.getOffers(), false),
                stats.getOrDefault(t.getId(), RatingService.Stats.NONE))).toList();
    }

    @Transactional
    public TutorCardResponse createTutor(CreateTutorRequest r) {
        User u = users.findById(r.userId()).orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "Пользователь не найден"));
        if (u.getRole() == Role.ADMIN)
            throw new BadRequestException("ADMIN_CANNOT_BE_TUTOR", "Администратор не может быть репетитором");
        if (tutors.existsByUserId(u.getId()))
            throw new ConflictException("TUTOR_EXISTS", "Профиль репетитора уже создан");
        u.setRole(Role.TUTOR);
        Tutor t = new Tutor();
        t.setUser(u); t.setBio(r.bio()); t.setPhotoUrl(r.photoUrl()); t.setExperienceYears(r.experienceYears());
        tutors.save(t);
        return TutorCardResponse.of(t, List.of(), RatingService.Stats.NONE);
    }

    @Transactional
    public TutorCardResponse updateTutor(Long id, UpdateTutorRequest r) {
        Tutor t = tutors.findWithDetailsById(id).orElseThrow(() -> new NotFoundException("TUTOR_NOT_FOUND", "Репетитор не найден"));
        t.setBio(r.bio()); t.setPhotoUrl(r.photoUrl());
        t.setExperienceYears(r.experienceYears()); t.setActive(r.active());
        var s = ratings.tutorStats(List.of(id)).getOrDefault(id, RatingService.Stats.NONE);
        return TutorCardResponse.of(t, OfferResponse.listOf(t.getOffers(), false), s);
    }
}
