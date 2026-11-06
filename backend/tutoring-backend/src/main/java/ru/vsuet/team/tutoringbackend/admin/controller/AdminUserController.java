package ru.vsuet.team.tutoringbackend.admin.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ru.vsuet.team.tutoringbackend.auth.AuthUser;
import ru.vsuet.team.tutoringbackend.user.Role;
import ru.vsuet.team.tutoringbackend.user.dto.UpdateUserRequest;
import ru.vsuet.team.tutoringbackend.user.dto.UserResponse;
import ru.vsuet.team.tutoringbackend.user.service.UserService;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {
    private final UserService users;

    @GetMapping
    public List<UserResponse> list(@RequestParam(required = false) Role role, @RequestParam(required = false) String q) {
        return users.list(role, q);
    }

    @PatchMapping("/{id}")
    public UserResponse update(@AuthenticationPrincipal AuthUser me, @PathVariable Long id,
                               @RequestBody UpdateUserRequest r) {
        return users.adminUpdate(me.id(), id, r);
    }
}
