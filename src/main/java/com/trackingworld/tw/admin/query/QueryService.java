package com.trackingworld.tw.admin.query;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trackingworld.tw.admin.dto.FilterRequest;
import com.trackingworld.tw.admin.dto.PageResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** POST /admin/query/{entity} and POST /admin/export/{entity}. */
@Slf4j
@Service
public class QueryService {

    public static final int EXPORT_MAX_ROWS = 50_000;
    private static final int EXPORT_PAGE_SIZE = 1000;

    private final EntityRegistry registry;
    private final SpecificationBuilder specificationBuilder;
    private final TransactionTemplate readOnlyTx;
    private final ObjectMapper objectMapper;

    public QueryService(EntityRegistry registry, SpecificationBuilder specificationBuilder,
                        PlatformTransactionManager transactionManager, ObjectMapper objectMapper) {
        this.registry = registry;
        this.specificationBuilder = specificationBuilder;
        this.readOnlyTx = new TransactionTemplate(transactionManager);
        this.readOnlyTx.setReadOnly(true);
        this.objectMapper = objectMapper;
    }

    public PageResult<Object> query(String entity, FilterRequest request) {
        EntityDef<?> def = registry.require(entity);
        return queryTyped(def, request);
    }

    private <T> PageResult<Object> queryTyped(EntityDef<T> def, FilterRequest request) {
        Specification<T> spec = specificationBuilder.build(def, request);
        PageRequest pageable = PageRequest.of(request.pageOrDefault(), request.sizeOrDefault(), specificationBuilder.sort(def, request));
        // Row mapping touches lazy associations, so it runs inside the read-only transaction.
        return readOnlyTx.execute(status -> {
            Page<T> page = def.repository().findAll(spec, pageable);
            List<Object> items = page.getContent().stream().map(def.rowMapper()).toList();
            return new PageResult<>(items, page.getNumber(), page.getSize(), page.getTotalElements());
        });
    }

    /** Resolves the entity and validates fields/operators up front (before the CSV response is committed). */
    public void validate(String entity, FilterRequest request) {
        validateTyped(registry.require(entity), request);
    }

    private <T> void validateTyped(EntityDef<T> def, FilterRequest request) {
        specificationBuilder.build(def, request);
        specificationBuilder.sort(def, request);
    }

    /** Streams CSV (UTF-8 with BOM for Excel), paging through the result up to EXPORT_MAX_ROWS. */
    public void exportCsv(String entity, FilterRequest request, OutputStream out) throws IOException {
        EntityDef<?> def = registry.require(entity);
        exportTyped(def, request, out);
    }

    private <T> void exportTyped(EntityDef<T> def, FilterRequest request, OutputStream out) throws IOException {
        Specification<T> spec = specificationBuilder.build(def, request);
        Writer writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
        writer.write('﻿');
        List<String> columns = null;
        int written = 0;
        int pageNo = 0;
        long start = System.currentTimeMillis();
        while (written < EXPORT_MAX_ROWS) {
            PageRequest pageable = PageRequest.of(pageNo, EXPORT_PAGE_SIZE, specificationBuilder.sort(def, request));
            List<Object> rows = readOnlyTx.execute(status ->
                    def.repository().findAll(spec, pageable).getContent().stream().map(def.rowMapper()).toList());
            if (rows == null || rows.isEmpty()) {
                break;
            }
            for (Object row : rows) {
                Map<String, Object> flat = objectMapper.convertValue(row, new TypeReference<LinkedHashMap<String, Object>>() {
                });
                if (columns == null) {
                    columns = new ArrayList<>(flat.keySet());
                    writeLine(writer, columns);
                }
                List<String> values = new ArrayList<>(columns.size());
                for (String column : columns) {
                    values.add(cell(flat.get(column)));
                }
                writeLine(writer, values);
                written++;
                if (written >= EXPORT_MAX_ROWS) {
                    break;
                }
            }
            if (rows.size() < EXPORT_PAGE_SIZE) {
                break;
            }
            pageNo++;
        }
        if (columns == null) {
            writeLine(writer, List.of("(no rows)"));
        }
        writer.flush();
        log.info("[ADMIN][EXPORT] entity={} rows={} in {} ms", def.name(), written, System.currentTimeMillis() - start);
    }

    private static void writeLine(Writer writer, List<String> values) throws IOException {
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                writer.write(',');
            }
            writer.write(escapeCsv(values.get(i)));
        }
        writer.write("\r\n");
    }

    private String cell(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof Instant i) {
            return i.toString();
        }
        if (value instanceof Map<?, ?> || value instanceof List<?>) {
            try {
                return objectMapper.writeValueAsString(value);
            } catch (IOException e) {
                return String.valueOf(value);
            }
        }
        return String.valueOf(value);
    }

    static String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        // Neutralise spreadsheet formula injection (but leave signed numbers and +92... phone numbers alone).
        String v = value;
        char first = v.isEmpty() ? ' ' : v.charAt(0);
        boolean signedNumber = (first == '+' || first == '-') && v.length() > 1
                && (Character.isDigit(v.charAt(1)) || v.charAt(1) == '.');
        boolean formulaLike = "=+-@\t\r".indexOf(first) >= 0 && !signedNumber;
        if (formulaLike) {
            v = "'" + v;
        }
        boolean needsQuotes = v.contains(",") || v.contains("\"") || v.contains("\n") || v.contains("\r");
        if (!needsQuotes) {
            return v;
        }
        return "\"" + v.replace("\"", "\"\"") + "\"";
    }
}
