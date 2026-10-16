package com.trackingworld.tw.admin.query;

import com.trackingworld.tw.admin.dto.FilterRequest;
import com.trackingworld.tw.common.ErrorCodes;
import com.trackingworld.tw.common.exceptions.BadRequestException;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Turns a {@link FilterRequest} into a JPA Specification. Fields are validated against the entity whitelist
 * (400 UNKNOWN_FIELD); nested paths such as {@code notification.user.phoneE164} become LEFT joins.
 */
@Component
public class SpecificationBuilder {

    public <T> Specification<T> build(EntityDef<T> def, FilterRequest request) {
        for (FilterRequest.Filter f : request.filtersOrEmpty()) {
            requireField(def, f.field());
        }
        for (FilterRequest.SortSpec s : request.sortOrEmpty()) {
            requireField(def, s.field());
        }
        return (root, query, cb) -> {
            Map<String, From<?, ?>> joins = new HashMap<>();
            List<Predicate> predicates = new ArrayList<>();
            for (FilterRequest.Filter f : request.filtersOrEmpty()) {
                predicates.add(predicate(cb, path(root, joins, f.field()), f));
            }
            Predicate base = predicates.isEmpty()
                    ? cb.conjunction()
                    : (request.isOr() ? cb.or(predicates.toArray(new Predicate[0])) : cb.and(predicates.toArray(new Predicate[0])));
            String search = request.search() == null ? "" : request.search().trim();
            if (search.isEmpty() || def.searchFields().isEmpty()) {
                return base;
            }
            String like = "%" + escapeLike(search.toLowerCase(Locale.ROOT)) + "%";
            List<Predicate> searchPredicates = new ArrayList<>();
            for (String field : def.searchFields()) {
                Path<?> p = path(root, joins, field);
                searchPredicates.add(cb.like(cb.lower(p.as(String.class)), like, '\\'));
            }
            return cb.and(base, cb.or(searchPredicates.toArray(new Predicate[0])));
        };
    }

    public Sort sort(EntityDef<?> def, FilterRequest request) {
        if (request.sortOrEmpty().isEmpty()) {
            return def.defaultSort();
        }
        List<Sort.Order> orders = new ArrayList<>();
        for (FilterRequest.SortSpec s : request.sortOrEmpty()) {
            requireField(def, s.field());
            Sort.Direction dir = s.dir() != null && s.dir().equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
            orders.add(new Sort.Order(dir, s.field()));
        }
        return Sort.by(orders);
    }

    private static void requireField(EntityDef<?> def, String field) {
        if (field == null || !def.allows(field)) {
            throw new BadRequestException(ErrorCodes.UNKNOWN_FIELD);
        }
    }

    static Path<?> path(Root<?> root, Map<String, From<?, ?>> joins, String dotted) {
        String[] segments = dotted.split("\\.");
        From<?, ?> from = root;
        StringBuilder prefix = new StringBuilder();
        for (int i = 0; i < segments.length - 1; i++) {
            String segment = segments[i];
            prefix.append(prefix.length() == 0 ? "" : ".").append(segment);
            String key = prefix.toString();
            From<?, ?> parent = from;
            from = joins.computeIfAbsent(key, k -> parent.join(segment, JoinType.LEFT));
        }
        return from.get(segments[segments.length - 1]);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Predicate predicate(CriteriaBuilder cb, Path<?> path, FilterRequest.Filter f) {
        String op = f.op() == null ? "" : f.op().trim();
        Class<?> type = path.getJavaType();
        switch (op) {
            case "isNull":
                return cb.isNull(path);
            case "notNull":
                return cb.isNotNull(path);
            case "eq":
                return cb.equal(path, coerce(type, f.value()));
            case "neq":
                return cb.notEqual(path, coerce(type, f.value()));
            case "contains":
                return cb.like(cb.lower(path.as(String.class)),
                        "%" + escapeLike(String.valueOf(f.value()).toLowerCase(Locale.ROOT)) + "%", '\\');
            case "startsWith":
                return cb.like(cb.lower(path.as(String.class)),
                        escapeLike(String.valueOf(f.value()).toLowerCase(Locale.ROOT)) + "%", '\\');
            case "gt":
                return cb.greaterThan((Expression<Comparable>) path, (Comparable) coerceComparable(type, f.value()));
            case "gte":
                return cb.greaterThanOrEqualTo((Expression<Comparable>) path, (Comparable) coerceComparable(type, f.value()));
            case "lt":
                return cb.lessThan((Expression<Comparable>) path, (Comparable) coerceComparable(type, f.value()));
            case "lte":
                return cb.lessThanOrEqualTo((Expression<Comparable>) path, (Comparable) coerceComparable(type, f.value()));
            case "between": {
                List<Object> bounds = asList(f.value());
                if (bounds.size() != 2) {
                    throw new BadRequestException(ErrorCodes.INVALID_FILTER_VALUE);
                }
                return cb.between((Expression<Comparable>) path, (Comparable) coerceComparable(type, bounds.get(0)),
                        (Comparable) coerceComparable(type, bounds.get(1)));
            }
            case "in": {
                List<Object> values = asList(f.value());
                if (values.isEmpty()) {
                    throw new BadRequestException(ErrorCodes.INVALID_FILTER_VALUE);
                }
                CriteriaBuilder.In<Object> in = cb.in((Expression<Object>) path);
                for (Object v : values) {
                    in.value(coerce(type, v));
                }
                return in;
            }
            default:
                throw new BadRequestException(ErrorCodes.UNKNOWN_OPERATOR);
        }
    }

    private static List<Object> asList(Object value) {
        if (value instanceof Collection<?> c) {
            return new ArrayList<>(c);
        }
        if (value instanceof Object[] arr) {
            return List.of(arr);
        }
        if (value instanceof String s && s.contains(",")) {
            List<Object> out = new ArrayList<>();
            for (String part : s.split(",")) {
                out.add(part.trim());
            }
            return out;
        }
        return value == null ? List.of() : List.of(value);
    }

    private static Object coerceComparable(Class<?> type, Object value) {
        Object coerced = coerce(type, value);
        if (!(coerced instanceof Comparable)) {
            throw new BadRequestException(ErrorCodes.INVALID_FILTER_VALUE);
        }
        return coerced;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    static Object coerce(Class<?> type, Object value) {
        if (value == null) {
            throw new BadRequestException(ErrorCodes.INVALID_FILTER_VALUE);
        }
        try {
            String text = String.valueOf(value).trim();
            if (type == String.class) {
                return text;
            }
            if (type == Long.class || type == long.class) {
                return value instanceof Number n ? n.longValue() : Long.parseLong(text);
            }
            if (type == Integer.class || type == int.class) {
                return value instanceof Number n ? n.intValue() : Integer.parseInt(text);
            }
            if (type == Double.class || type == double.class) {
                return value instanceof Number n ? n.doubleValue() : Double.parseDouble(text);
            }
            if (type == Boolean.class || type == boolean.class) {
                if (value instanceof Boolean b) {
                    return b;
                }
                if (text.equalsIgnoreCase("true") || text.equalsIgnoreCase("false")) {
                    return Boolean.parseBoolean(text);
                }
                throw new BadRequestException(ErrorCodes.INVALID_FILTER_VALUE);
            }
            if (type == Instant.class) {
                return parseInstant(text);
            }
            if (type.isEnum()) {
                return Enum.valueOf((Class<Enum>) type, text.toUpperCase(Locale.ROOT));
            }
            return text;
        } catch (DateTimeParseException | IllegalArgumentException e) {
            throw new BadRequestException(ErrorCodes.INVALID_FILTER_VALUE);
        }
    }

    static Instant parseInstant(String text) {
        if (text.length() == 10) {
            return LocalDate.parse(text).atStartOfDay(ZoneOffset.UTC).toInstant();
        }
        try {
            return Instant.parse(text);
        } catch (DateTimeParseException e) {
            return java.time.OffsetDateTime.parse(text).toInstant();
        }
    }

    static String escapeLike(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
