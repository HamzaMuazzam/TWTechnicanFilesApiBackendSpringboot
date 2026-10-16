package com.trackingworld.tw.admin;

import com.trackingworld.tw.admin.dto.Rows;
import com.trackingworld.tw.admin.query.RowMappers;
import com.trackingworld.tw.alerts.repo.ConfigurationRepository;
import com.trackingworld.tw.common.ApiPaths;
import com.trackingworld.tw.common.ErrorCodes;
import com.trackingworld.tw.common.ResponseWrapper;
import com.trackingworld.tw.common.exceptions.NotFoundException;
import com.trackingworld.tw.ingest.IngestService;
import com.trackingworld.tw.ingest.dto.IngestConfigurationRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Dashboard add/delete of configuration rows (the grid uses /admin/query/configurations). */
@Slf4j
@RestController
@RequestMapping(ApiPaths.ADMIN + "/configurations")
@RequiredArgsConstructor
@Tag(name = "Admin - Configurations")
public class AdminConfigurationController {

    private final IngestService ingestService;
    private final ConfigurationRepository repository;

    @Operation(summary = "Add (or upsert by S3 id) a configuration row")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseWrapper<Rows.ConfigurationRow> create(@Valid @RequestBody IngestConfigurationRequest request) {
        return ResponseWrapper.success(RowMappers.configuration(ingestService.saveConfiguration(request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseWrapper<Boolean> delete(@PathVariable Long id) {
        if (!repository.existsById(id)) {
            throw new NotFoundException(ErrorCodes.NOT_FOUND);
        }
        repository.deleteById(id);
        log.info("[ADMIN] configuration #{} deleted", id);
        return ResponseWrapper.success(true);
    }
}
