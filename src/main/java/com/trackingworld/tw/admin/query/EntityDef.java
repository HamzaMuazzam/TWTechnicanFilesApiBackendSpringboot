package com.trackingworld.tw.admin.query;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Set;
import java.util.function.Function;

/**
 * One queryable entity: its repository, the whitelisted (dotted) field paths, the fields used by free-text
 * search, the default sort and the entity -> flat row mapper.
 */
public record EntityDef<T>(
        String name,
        Class<T> entityClass,
        JpaSpecificationExecutor<T> repository,
        Set<String> fields,
        Set<String> searchFields,
        Sort defaultSort,
        Function<T, Object> rowMapper
) {
    public boolean allows(String field) {
        return fields.contains(field);
    }
}
