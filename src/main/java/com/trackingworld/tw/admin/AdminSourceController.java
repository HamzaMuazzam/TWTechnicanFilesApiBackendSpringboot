package com.trackingworld.tw.admin;

import com.trackingworld.tw.admin.dto.Rows;
import com.trackingworld.tw.admin.dto.SourceCreateRequest;
import com.trackingworld.tw.admin.dto.SourcePatchRequest;
import com.trackingworld.tw.admin.dto.SourceWithKeyDto;
import com.trackingworld.tw.admin.query.RowMappers;
import com.trackingworld.tw.alerts.domain.IngestSource;
import com.trackingworld.tw.alerts.repo.IngestSourceRepository;
import com.trackingworld.tw.common.ApiPaths;
import com.trackingworld.tw.common.ErrorCodes;
import com.trackingworld.tw.common.ResponseWrapper;
import com.trackingworld.tw.common.exceptions.ConflictException;
import com.trackingworld.tw.common.exceptions.NotFoundException;
import com.trackingworld.tw.security.AdminPrincipal;
import com.trackingworld.tw.security.ApiKeys;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping(ApiPaths.ADMIN + "/sources")
@RequiredArgsConstructor
@Tag(name = "Admin - Ingest sources")
public class AdminSourceController {

    private final IngestSourceRepository sourceRepository;

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseWrapper<List<Rows.SourceRow>> list() {
        return ResponseWrapper.success(sourceRepository.findAll(Sort.by("id")).stream().map(RowMappers::source).toList());
    }

    @Operation(summary = "Create a source; the plaintext API key is returned once")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @Transactional
    public ResponseWrapper<SourceWithKeyDto> create(@Valid @RequestBody SourceCreateRequest request,
                                                    @AuthenticationPrincipal AdminPrincipal principal) {
        String name = request.name().trim();
        if (sourceRepository.existsByNameIgnoreCase(name)) {
            throw new ConflictException(ErrorCodes.SOURCE_EXISTS);
        }
        String apiKey = ApiKeys.generate();
        IngestSource source = sourceRepository.save(IngestSource.builder()
                .name(name)
                .apiKeyHash(ApiKeys.hash(apiKey))
                .apiKeyPrefix(ApiKeys.prefix(apiKey))
                .active(true)
                .build());
        log.info("[ADMIN] ingest source '{}' created by {} (key prefix {})", name, principal.username(), source.getApiKeyPrefix());
        return ResponseWrapper.success(new SourceWithKeyDto(RowMappers.source(source), apiKey));
    }

    @Operation(summary = "Rotate the API key; the new plaintext key is returned once")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/rotate-key")
    @Transactional
    public ResponseWrapper<SourceWithKeyDto> rotate(@PathVariable Long id, @AuthenticationPrincipal AdminPrincipal principal) {
        IngestSource source = require(id);
        String apiKey = ApiKeys.generate();
        source.setApiKeyHash(ApiKeys.hash(apiKey));
        source.setApiKeyPrefix(ApiKeys.prefix(apiKey));
        log.warn("[ADMIN] ingest source '{}' API key ROTATED by {} (new prefix {})", source.getName(), principal.username(),
                source.getApiKeyPrefix());
        return ResponseWrapper.success(new SourceWithKeyDto(RowMappers.source(source), apiKey));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}")
    @Transactional
    public ResponseWrapper<Rows.SourceRow> patch(@PathVariable Long id, @Valid @RequestBody SourcePatchRequest request,
                                                 @AuthenticationPrincipal AdminPrincipal principal) {
        IngestSource source = require(id);
        source.setActive(request.active());
        log.info("[ADMIN] ingest source '{}' active={} by {}", source.getName(), request.active(), principal.username());
        return ResponseWrapper.success(RowMappers.source(source));
    }

    private IngestSource require(Long id) {
        return sourceRepository.findById(id).orElseThrow(() -> new NotFoundException(ErrorCodes.NOT_FOUND));
    }
}
