package com.trackingworld.tw.admin;

import com.trackingworld.tw.admin.dto.OverviewDto;
import com.trackingworld.tw.common.config.AppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** GET /admin/overview - aggregate KPIs over [from, to). */
@Service
@RequiredArgsConstructor
public class OverviewService {

    private final JdbcTemplate jdbc;
    private final AppProperties appProperties;

    @Transactional(readOnly = true)
    public OverviewDto overview(Instant from, Instant to) {
        Timestamp f = Timestamp.from(from);
        Timestamp t = Timestamp.from(to);
        String tz = appProperties.timezone();

        long alertsTotal = count("select count(*) from alert where sdt >= ? and sdt < ?", f, t);
        Map<String, Long> byStatus = new TreeMap<>();
        jdbc.query("select status, count(*) from notification where created_at >= ? and created_at < ? group by status",
                rs -> {
                    byStatus.put(rs.getString(1), rs.getLong(2));
                }, f, t);
        long notificationsTotal = byStatus.values().stream().mapToLong(Long::longValue).sum();
        long sent = byStatus.getOrDefault("SENT", 0L);
        long delivered = byStatus.getOrDefault("DELIVERED", 0L);
        long read = byStatus.getOrDefault("READ", 0L);
        long failed = byStatus.getOrDefault("FAILED", 0L);
        long noDevice = byStatus.getOrDefault("NO_DEVICE", 0L);
        long discarded = count("select count(*) from discarded_recipient where created_at >= ? and created_at < ?", f, t);
        long usersTotal = count("select count(*) from app_user");
        long usersNew = count("select count(*) from app_user where created_at >= ? and created_at < ?", f, t);
        long devicesActive = count("select count(*) from user_device where active = true and fcm_token is not null");
        long devicesSilent = count("select count(*) from user_device where active = true and (last_seen_at is null or last_seen_at < ?)",
                Timestamp.from(Instant.now().minus(Duration.ofDays(3))));

        double deliveryRate = notificationsTotal == 0 ? 0 : round((delivered + read) * 100.0 / notificationsTotal);
        double readRate = notificationsTotal == 0 ? 0 : round(read * 100.0 / notificationsTotal);

        Map<String, long[]> perDay = new TreeMap<>();
        jdbc.query("select to_char(date_trunc('day', sdt at time zone ?), 'YYYY-MM-DD') d, count(*) "
                        + "from alert where sdt >= ? and sdt < ? group by 1 order by 1",
                rs -> {
                    perDay.computeIfAbsent(rs.getString(1), k -> new long[4])[0] = rs.getLong(2);
                }, tz, f, t);
        jdbc.query("select to_char(date_trunc('day', created_at at time zone ?), 'YYYY-MM-DD') d, "
                        + "count(*) filter (where status in ('DELIVERED','READ')) delivered_count, "
                        + "count(*) filter (where status = 'READ') read_count, "
                        + "count(*) filter (where status = 'FAILED') failed_count "
                        + "from notification where created_at >= ? and created_at < ? group by 1 order by 1",
                rs -> {
                    long[] row = perDay.computeIfAbsent(rs.getString(1), k -> new long[4]);
                    row[1] = rs.getLong(2);
                    row[2] = rs.getLong(3);
                    row[3] = rs.getLong(4);
                }, tz, f, t);
        List<OverviewDto.PerDay> perDayList = new ArrayList<>();
        perDay.forEach((date, v) -> perDayList.add(new OverviewDto.PerDay(date, v[0], v[1], v[2], v[3])));

        List<OverviewDto.ByType> byType = jdbc.query(
                "select a.alert_type_code, coalesce(t.label, a.alert_type_code), count(*) "
                        + "from alert a left join alert_type t on t.code = a.alert_type_code "
                        + "where a.sdt >= ? and a.sdt < ? group by 1, 2 order by 3 desc",
                (rs, i) -> new OverviewDto.ByType(rs.getString(1), rs.getString(2), rs.getLong(3)), f, t);

        return new OverviewDto(alertsTotal, notificationsTotal, sent, delivered, read, failed, noDevice, discarded,
                usersTotal, usersNew, devicesActive, devicesSilent, deliveryRate, readRate, perDayList, byType);
    }

    private long count(String sql, Object... args) {
        Long v = jdbc.queryForObject(sql, Long.class, args);
        return v == null ? 0 : v;
    }

    private static double round(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
