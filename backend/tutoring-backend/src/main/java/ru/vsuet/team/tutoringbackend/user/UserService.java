package ru.vsuet.team.tutoringbackend.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.vsuet.team.tutoringbackend.catalog.repository.TutorRepository;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final TutorRepository tutorRepository;
}
