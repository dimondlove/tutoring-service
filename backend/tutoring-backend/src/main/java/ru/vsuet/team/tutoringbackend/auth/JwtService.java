package ru.vsuet.team.tutoringbackend.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;
import ru.vsuet.team.tutoringbackend.config.AppProperties;
import ru.vsuet.team.tutoringbackend.user.User;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {
    private final SecretKey secretKey;
    private final Duration ttl;
    private final Clock clock;

    public JwtService(AppProperties appProperties, Clock clock) {
        this.secretKey = Keys.hmacShaKeyFor(appProperties.jwt().secret().getBytes(StandardCharsets.UTF_8));
        this.ttl = Duration.ofMinutes(appProperties.jwt().ttlMinutes());
        this.clock = clock;
    }

    public String generate(User user) {
        Instant now = clock.instant();
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("role", user.getRole().name())
                .issuedAt(Date.from(now.plus(ttl)))
                .signWith(secretKey)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser().verifyWith(secretKey).clock(() -> Date.from(clock.instant())).build().parseSignedClaims(token).getPayload();
    }
}
