package com.trackingworld.tw.admin.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/** SPEC §7.5 generic grid query. */
public record FilterRequest(
        @Size(max = 50) List<@Valid Filter> filters,
        @Pattern(regexp = "AND|OR", message = "must be AND or OR") String logic,
        @Size(max = 5) List<@Valid SortSpec> sort,
        @Min(0) Integer page,
        @Min(1) @Max(200) Integer size,
        @Size(max = 200) String search
) {
    public static final int MAX_SIZE = 200;
    public static final int DEFAULT_SIZE = 50;

    public record Filter(@NotBlank @Size(max = 100) String field,
                         @NotBlank @Size(max = 20) String op,
                         Object value) {
    }

    public record SortSpec(@NotBlank @Size(max = 100) String field,
                           @Pattern(regexp = "(?i)asc|desc", message = "must be asc or desc") String dir) {
    }

    public List<Filter> filtersOrEmpty() {
        return filters == null ? List.of() : filters;
    }

    public List<SortSpec> sortOrEmpty() {
        return sort == null ? List.of() : sort;
    }

    public boolean isOr() {
        return "OR".equalsIgnoreCase(logic);
    }

    public int pageOrDefault() {
        return page == null ? 0 : page;
    }

    public int sizeOrDefault() {
        return size == null ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
    }
}
