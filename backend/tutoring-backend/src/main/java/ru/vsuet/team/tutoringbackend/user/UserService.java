package ru.vsuet.team.tutoringbackend.user;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.vsuet.team.tutoringbackend.catalog.repository.TutorRepository;
import ru.vsuet.team.tutoringbackend.common.BadRequestException;
import ru.vsuet.team.tutoringbackend.common.ConflictException;
import ru.vsuet.team.tutoringbackend.common.NotFoundException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final TutorRepository tutorRepository;

    @Transactional(readOnly = true)
    public UserResponse get(Long id) { return UserResponse.from(find(id)); }

    @Transactional
    public UserResponse updateProfile(Long id, UpdateProfileRequest r) {
        User u = find(id);
        u.setFullName(r.fullName().trim());
        u.setPhone(r.phone());
        u.setBirthDate(r.birthDate());
        return UserResponse.from(u);
    }

    /** Фильтрация в Java: для админки прототипа этого достаточно. */
    @Transactional(readOnly = true)
    public List<UserResponse> list(Role role, String q) {
        String query = q == null ? "" : q.trim().toLowerCase();
        return userRepository.findAll(Sort.by("id")).stream()
                .filter(u -> role == null || u.getRole() == role)
                .filter(u -> query.isEmpty() || u.getEmail().toLowerCase().contains(query)
                        || u.getFullName().toLowerCase().contains(query))
                .map(UserResponse::from).toList();
    }

    @Transactional
    public UserResponse adminUpdate(Long actorId, Long targetId, UpdateUserRequest r) {
        if (actorId.equals(targetId))
            throw new BadRequestException("CANNOT_MODIFY_SELF", "Нельзя менять собственную роль или блокировку");
        User u = find(targetId);
        if (r.role() != null) {
            boolean hasProfile = tutorRepository.existsByUserId(targetId);
            if (hasProfile && r.role() != Role.TUTOR)
                throw new ConflictException("USER_HAS_TUTOR_PROFILE", "Сначала деактивируйте профиль репетитора");
            if (!hasProfile && r.role() == Role.TUTOR)
                throw new BadRequestException("USE_TUTOR_CREATION", "Роль TUTOR выдаётся при создании профиля репетитора");
            u.setRole(r.role());
        }
        if (r.enabled() != null) u.setEnabled(r.enabled());
        return UserResponse.from(u);
    }

    private User find(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "Пользователь не найден"));
    }
}
