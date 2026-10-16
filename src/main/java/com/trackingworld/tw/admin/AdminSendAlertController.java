package com.trackingworld.tw.admin;

import com.trackingworld.tw.alerts.domain.IngestSource;
import com.trackingworld.tw.alerts.repo.IngestSourceRepository;
import com.trackingworld.tw.common.ApiPaths;
import com.trackingworld.tw.common.ResponseWrapper;
import com.trackingworld.tw.ingest.IngestService;
import com.trackingworld.tw.ingest.dto.IngestAlertRequest;
import com.trackingworld.tw.ingest.dto.IngestResultDto;
import com.trackingworld.tw.security.AdminPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

/**
 * Lets the dashboard post an alert exactly the way S3 will (same IngestService, same configuration lookup),
 * so the whole pipeline can be tested before the external APIs are handed over.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "Admin - Alerts")
public class AdminSendAlertController {

    private final IngestService ingestService;
    private final IngestSourceRepository sourceRepository;

    @Operation(summary = "Send an alert from the dashboard (goes through the normal ingest pipeline)")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(ApiPaths.ADMIN + "/alerts/send")
    public ResponseWrapper<IngestResultDto> send(@Valid @RequestBody IngestAlertRequest request,
                                                 @AuthenticationPrincipal AdminPrincipal principal) {
        IngestSource source = sourceRepository.findAll().stream()
                .filter(s -> "dashboard".equalsIgnoreCase(s.getName())).findFirst()
                .orElseGet(() -> sourceRepository.save(IngestSource.builder()
                        .name("dashboard").apiKeyHash("n/a").apiKeyPrefix("dash").active(true)
                        .createdAt(Instant.now()).build()));
        IngestAlertRequest item = request.id() == null || request.id().isBlank()
                ? new IngestAlertRequest("DASH-" + System.currentTimeMillis(), request.uid(), request.alertType(),
                        request.message(), request.lat(), request.lon(), request.dbdt(), request.extra())
                : request;
        log.info("[ADMIN] {} sends alert uid={} type={}", principal.username(), item.uid(), item.alertType());
        return ResponseWrapper.success(ingestService.ingest(source.getId(), List.of(item)).get(0));
    }
}
