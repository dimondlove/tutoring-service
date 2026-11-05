package ru.vsuet.team.tutoringbackend.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.vsuet.team.tutoringbackend.common.ConflictException;
import ru.vsuet.team.tutoringbackend.common.UnauthorizedException;
import ru.vsuet.team.tutoringbackend.user.Role;
import ru.vsuet.team.tutoringbackend.user.User;
import ru.vsuet.team.tutoringbackend.user.UserRepository;
import ru.vsuet.team.tutoringbackend.user.UserResponse;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    @Transactional
    public AuthResponse register(RegisterRequest r) {
        String email = r.email().trim().toLowerCase();
        if (users.existsByEmail(email))
            throw new ConflictException("EMAIL_TAKEN", "Пользователь с таким email уже существует");
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(encoder.encode(r.password()));
        user.setFullName(r.fullName().trim());
        user.setPhone(r.phone());
        user.setBirthDate(r.birthDate());
        user.setRole(Role.STUDENT); // регистрация всегда создаёт ученика
        try {
            users.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("EMAIL_TAKEN", "Пользователь с таким email уже существует");
        }
        return new AuthResponse(jwt.generate(user), UserResponse.from(user));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest r) {
        User u = users.findByEmail(r.email().trim().toLowerCase())
                .filter(User::isEnabled)
                .filter(x -> encoder.matches(r.password(), x.getPasswordHash()))
                .orElseThrow(() -> new UnauthorizedException("BAD_CREDENTIALS", "Неверный email или пароль"));
        return new AuthResponse(jwt.generate(u), UserResponse.from(u));
    }
}
