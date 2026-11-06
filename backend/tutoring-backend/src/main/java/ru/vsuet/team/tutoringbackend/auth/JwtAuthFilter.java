package ru.vsuet.team.tutoringbackend.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import ru.vsuet.team.tutoringbackend.auth.service.JwtService;
import ru.vsuet.team.tutoringbackend.user.User;
import ru.vsuet.team.tutoringbackend.user.repository.UserRepository;

import java.io.IOException;
import java.util.List;

@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String header = req.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                Claims c = jwtService.parse(header.substring(7));
                userRepository.findById(Long.valueOf(c.getSubject())).filter(User::isEnabled).ifPresent(u -> {
                    var principal = new AuthUser(u.getId(), u.getEmail(), u.getRole());
                    var auth = new UsernamePasswordAuthenticationToken(principal, null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + u.getRole().name())));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                });
            } catch (JwtException | IllegalArgumentException ignored) {
                // невалидный или просроченный токен: запрос остаётся анонимным, защищённые пути ответят 401
            }
        }
        chain.doFilter(req, res);
    }

}

