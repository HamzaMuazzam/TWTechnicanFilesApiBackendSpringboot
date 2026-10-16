package com.trackingworld.tw.admin;

import com.trackingworld.tw.admin.dto.AlertTypeRequest;
import com.trackingworld.tw.admin.dto.Rows;
import com.trackingworld.tw.admin.query.RowMappers;
import com.trackingworld.tw.alerts.domain.AlertType;
import com.trackingworld.tw.alerts.domain.Severity;
import com.trackingworld.tw.alerts.repo.AlertRepository;
import com.trackingworld.tw.alerts.repo.AlertTypeRepository;
import com.trackingworld.tw.common.ApiPaths;
import com.trackingworld.tw.common.ErrorCodes;
import com.trackingworld.tw.common.ResponseWrapper;
import com.trackingworld.tw.common.exceptions.ConflictException;
import com.trackingworld.tw.common.exceptions.NotFoundException;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping(ApiPaths.ADMIN + "/alert-types")
@RequiredArgsConstructor
@Tag(name = "Admin - Alert types")
public class AdminAlertTypeController {

    private final AlertTypeRepository alertTypeRepository;
    private final AlertRepository alertRepository;

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseWrapper<List<Rows.AlertTypeRow>> list() {
        return ResponseWrapper.success(alertTypeRepository.findAllByOrderBySortOrderAscCodeAsc().stream()
                .map(RowMappers::alertType).toList());
    }

    @GetMapping("/{code}")
    @Transactional(readOnly = true)
    public ResponseWrapper<Rows.AlertTypeRow> get(@PathVariable String code) {
        return ResponseWrapper.success(RowMappers.alertType(require(code)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @Transactional
    public ResponseWrapper<Rows.AlertTypeRow> create(@Valid @RequestBody AlertTypeRequest request) {
        String code = request.code().trim();
        if (alertTypeRepository.existsById(code)) {
            throw new ConflictException(ErrorCodes.ALERT_TYPE_EXISTS);
        }
        AlertType type = AlertType.builder().code(code).build();
        apply(type, request);
        log.info("[ADMIN] alert type created {}", code);
        return ResponseWrapper.success(RowMappers.alertType(alertTypeRepository.save(type)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{code}")
    @Transactional
    public ResponseWrapper<Rows.AlertTypeRow> update(@PathVariable String code, @Valid @RequestBody AlertTypeRequest request) {
        AlertType type = require(code);
        apply(type, request);
        log.info("[ADMIN] alert type updated {}", code);
        return ResponseWrapper.success(RowMappers.alertType(type));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{code}")
    @Transactional
    public ResponseWrapper<Void> delete(@PathVariable String code) {
        AlertType type = require(code);
        if (alertRepository.existsByAlertTypeCode(code)) {
            throw new ConflictException(ErrorCodes.ALERT_TYPE_IN_USE);
        }
        alertTypeRepository.delete(type);
        log.info("[ADMIN] alert type deleted {}", code);
        return ResponseWrapper.success(null);
    }

    private AlertType require(String code) {
        return alertTypeRepository.findById(code).orElseThrow(() -> new NotFoundException(ErrorCodes.NOT_FOUND));
    }

    private static void apply(AlertType type, AlertTypeRequest r) {
        type.setLabel(r.label().trim());
        type.setSeverity(Severity.valueOf(r.severity()));
        type.setIcon(blankToNull(r.icon()));
        type.setColor(blankToNull(r.color()));
        type.setChannelId(blankToNull(r.channelId()));
        if (r.active() != null) {
            type.setActive(r.active());
        }
        if (r.sortOrder() != null) {
            type.setSortOrder(r.sortOrder());
        }
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
