package ru.vsuet.team.tutoringbackend.admin;

import lombok.RequiredArgsConstructor;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminStatsService {
    private final JdbcTemplate jdbc;
    private final Clock clock;

    @Transactional(readOnly = true)
    public AdminStats stats() {
        OffsetDateTime now = OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
        OffsetDateTime weekAgo = now.minusDays(7), weekAhead = now.plusDays(7);

        Map<String, Long> byRole = counts("select role as k, count(*) as v from users group by role");
        Map<String, Long> byStatus = counts("select status as k, count(*) as v from booking group by status");
        long newUsers = jdbc.queryForObject("select count(*) from users where created_at >= ?", Long.class, weekAgo);
        long upcoming = jdbc.queryForObject("""
                select count(*) from booking
                where status in ('PENDING','CONFIRMED') and start_time >= ? and start_time < ?""",
                Long.class, now, weekAhead);

        long total = byStatus.values().stream().mapToLong(Long::longValue).sum();
        long cancelled = byStatus.getOrDefault("CANCELLED", 0L);
        double rate = total == 0 ? 0 : Math.round(cancelled * 1000.0 / total) / 10.0;

        var topTutors = jdbc.query("""
                select t.id, u.full_name, count(b.id) as lessons, coalesce(avg(b.tutor_rating), 0) as rating
                from tutor t join users u on u.id = t.user_id
                left join booking b on b.tutor_id = t.id and b.status = 'COMPLETED'
                group by t.id, u.full_name
                order by lessons desc, rating desc limit 5""",
                (rs, i) -> new AdminStats.TopTutor(rs.getLong("id"), rs.getString("full_name"),
                        rs.getLong("lessons"), Math.round(rs.getDouble("rating") * 10) / 10.0));

        var topSubjects = jdbc.query("""
                select s.name, count(b.id) as cnt
                from subject s join booking b on b.subject_id = s.id
                where b.status in ('CONFIRMED','COMPLETED')
                group by s.name order by cnt desc limit 5""",
                (rs, i) -> new AdminStats.TopSubject(rs.getString("name"), rs.getLong("cnt")));

        return new AdminStats(byRole, byStatus, newUsers, upcoming, rate, topTutors, topSubjects);
    }

    private Map<String, Long> counts(String sql) {
        Map<String, Long> m = new LinkedHashMap<>();
        jdbc.queryForList(sql).forEach(r -> m.put((String) r.get("k"), ((Number) r.get("v")).longValue()));
        return m;
    }
}
