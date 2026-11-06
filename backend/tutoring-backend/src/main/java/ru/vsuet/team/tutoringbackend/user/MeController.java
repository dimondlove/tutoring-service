package ru.vsuet.team.tutoringbackend.user;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ru.vsuet.team.tutoringbackend.auth.AuthUser;
import ru.vsuet.team.tutoringbackend.user.dto.UpdateProfileRequest;
import ru.vsuet.team.tutoringbackend.user.dto.UserResponse;
import ru.vsuet.team.tutoringbackend.user.service.UserService;

@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class MeController {
    private final UserService users;

    @GetMapping
    public UserResponse me(@AuthenticationPrincipal AuthUser me) {
        return users.get(me.id());
    }

    @PutMapping
    public UserResponse update(@AuthenticationPrincipal AuthUser me,
                               @Valid @RequestBody UpdateProfileRequest r) {
        return users.updateProfile(me.id(), r);
    }
}
