package com.trackingworld.tw.admin.sql;

import com.trackingworld.tw.admin.dto.SqlResultDto;
import com.trackingworld.tw.common.ErrorCodes;
import com.trackingworld.tw.common.exceptions.BaseException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Array;
import java.sql.Date;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

/** POST /admin/sql - read-only SQL console (ADMIN only). */
@Slf4j
@Service
public class SqlConsoleService {

    public static final int MAX_ROWS = 1000;
    private static final String STATEMENT_TIMEOUT = "10s";

    private final JdbcTemplate jdbc;
    private final TransactionTemplate readOnlyTx;

    public SqlConsoleService(DataSource dataSource, PlatformTransactionManager transactionManager) {
        this.jdbc = new JdbcTemplate(dataSource);
        this.jdbc.setMaxRows(MAX_ROWS + 1);
        this.jdbc.setFetchSize(200);
        this.readOnlyTx = new TransactionTemplate(transactionManager);
        this.readOnlyTx.setReadOnly(true);
        this.readOnlyTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public SqlResultDto run(String rawSql, String adminUsername, String ip) {
        String sql;
        try {
            sql = SqlValidator.validate(rawSql);
        } catch (IllegalArgumentException e) {
            log.warn("[SQL-AUDIT] REJECTED admin={} ip={} reason='{}' sql={}", adminUsername, ip, e.getMessage(), oneLine(rawSql));
            throw new SqlConsoleException(HttpStatus.BAD_REQUEST, ErrorCodes.SQL_NOT_ALLOWED, e.getMessage());
        }
        long start = System.currentTimeMillis();
        try {
            SqlResultDto result = readOnlyTx.execute(status -> {
                jdbc.execute("SET TRANSACTION READ ONLY");
                jdbc.execute("SET LOCAL statement_timeout = '" + STATEMENT_TIMEOUT + "'");
                return jdbc.query(sql, rs -> {
                    ResultSetMetaData meta = rs.getMetaData();
                    int columnCount = meta.getColumnCount();
                    List<String> columns = new ArrayList<>(columnCount);
                    for (int i = 1; i <= columnCount; i++) {
                        columns.add(meta.getColumnLabel(i));
                    }
                    List<List<Object>> rows = new ArrayList<>();
                    boolean truncated = false;
                    while (rs.next()) {
                        if (rows.size() >= MAX_ROWS) {
                            truncated = true;
                            break;
                        }
                        List<Object> row = new ArrayList<>(columnCount);
                        for (int i = 1; i <= columnCount; i++) {
                            row.add(convert(rs.getObject(i)));
                        }
                        rows.add(row);
                    }
                    return new SqlResultDto(columns, rows, truncated, 0);
                });
            });
            long elapsed = System.currentTimeMillis() - start;
            log.info("[SQL-AUDIT] OK admin={} ip={} rows={} truncated={} elapsedMs={} sql={}", adminUsername, ip,
                    result.rows().size(), result.truncated(), elapsed, oneLine(sql));
            return new SqlResultDto(result.columns(), result.rows(), result.truncated(), elapsed);
        } catch (DataAccessException e) {
            String reason = e.getMostSpecificCause().getMessage();
            log.warn("[SQL-AUDIT] ERROR admin={} ip={} elapsedMs={} error='{}' sql={}", adminUsername, ip,
                    System.currentTimeMillis() - start, oneLine(reason), oneLine(sql));
            throw new SqlConsoleException(HttpStatus.BAD_REQUEST, ErrorCodes.SQL_ERROR, reason);
        }
    }

    static Object convert(Object value) throws SQLException {
        if (value == null || value instanceof String || value instanceof Boolean || value instanceof Integer
                || value instanceof Long || value instanceof Double || value instanceof Float || value instanceof Short
                || value instanceof BigDecimal) {
            return value;
        }
        if (value instanceof Timestamp ts) {
            return ts.toInstant().toString();
        }
        if (value instanceof OffsetDateTime odt) {
            return odt.toInstant().toString();
        }
        if (value instanceof LocalDateTime ldt) {
            return ldt.atOffset(ZoneOffset.UTC).toInstant().toString();
        }
        if (value instanceof Date d) {
            return d.toLocalDate().toString();
        }
        if (value instanceof Time t) {
            return t.toLocalTime().toString();
        }
        if (value instanceof byte[] bytes) {
            return Base64.getEncoder().encodeToString(bytes);
        }
        if (value instanceof Array array) {
            Object raw = array.getArray();
            if (raw instanceof Object[] objects) {
                List<Object> out = new ArrayList<>(objects.length);
                for (Object o : objects) {
                    out.add(convert(o));
                }
                return out;
            }
            return Arrays.toString((Object[]) raw);
        }
        if (value instanceof Number n) {
            return n;
        }
        return String.valueOf(value);
    }

    private static String oneLine(String s) {
        if (s == null) {
            return "";
        }
        String compact = s.replaceAll("\\s+", " ").trim();
        return compact.length() > 500 ? compact.substring(0, 500) + "..." : compact;
    }

    /** Carries the DB / validator reason in {@code data}. */
    public static class SqlConsoleException extends BaseException {
        private final String reason;

        public SqlConsoleException(HttpStatus status, String code, String reason) {
            super(status, code, code);
            this.reason = reason;
        }

        public String getReason() {
            return reason;
        }
    }
}
