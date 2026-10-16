package com.trackingworld.tw.admin.sql;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Read-only SQL console guard: single statement, starts with SELECT or WITH, no ';', no mutating keywords
 * (as whole words, case-insensitive). The statement also runs in a READ ONLY transaction with a 10s timeout,
 * so this is defence in depth, not the only barrier.
 */
public final class SqlValidator {

    private static final Pattern FORBIDDEN = Pattern.compile(
            "\\b(insert|update|delete|drop|alter|truncate|create|grant|copy|call|do|execute)\\b",
            Pattern.CASE_INSENSITIVE);

    private SqlValidator() {
    }

    /** @return the trimmed statement; throws {@link IllegalArgumentException} with a human reason when rejected. */
    public static String validate(String sql) {
        if (sql == null) {
            throw new IllegalArgumentException("SQL is empty");
        }
        String trimmed = sql.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("SQL is empty");
        }
        if (trimmed.indexOf(';') >= 0) {
            throw new IllegalArgumentException("Semicolons are not allowed (single statement only)");
        }
        String lower = trimmed.toLowerCase(Locale.ROOT);
        if (!(lower.startsWith("select") || lower.startsWith("with"))) {
            throw new IllegalArgumentException("Statement must start with SELECT or WITH");
        }
        if (lower.length() > 6 && (lower.startsWith("select") && Character.isLetterOrDigit(lower.charAt(6)))
                || (lower.startsWith("with") && lower.length() > 4 && Character.isLetterOrDigit(lower.charAt(4)))) {
            throw new IllegalArgumentException("Statement must start with SELECT or WITH");
        }
        var matcher = FORBIDDEN.matcher(trimmed);
        if (matcher.find()) {
            throw new IllegalArgumentException("Keyword not allowed: " + matcher.group(1).toUpperCase(Locale.ROOT));
        }
        return trimmed;
    }
}
