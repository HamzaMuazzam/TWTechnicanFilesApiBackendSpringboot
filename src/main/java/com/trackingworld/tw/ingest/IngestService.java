package com.trackingworld.tw.ingest;

import com.trackingworld.tw.alerts.dispatch.AlertAcceptedEvent;
import com.trackingworld.tw.alerts.domain.Alert;
import com.trackingworld.tw.alerts.domain.AlertType;
import com.trackingworld.tw.alerts.domain.AppUser;
import com.trackingworld.tw.alerts.domain.DeliveryStatus;
import com.trackingworld.tw.alerts.domain.DiscardReason;
import com.trackingworld.tw.alerts.domain.DiscardedRecipient;
import com.trackingworld.tw.alerts.domain.IngestSource;
import com.trackingworld.tw.alerts.domain.Notification;
import com.trackingworld.tw.alerts.domain.NotificationDelivery;
import com.trackingworld.tw.alerts.domain.NotificationStatus;
import com.trackingworld.tw.alerts.domain.Severity;
import com.trackingworld.tw.alerts.domain.UserDevice;
import com.trackingworld.tw.alerts.domain.UserStatus;
import com.trackingworld.tw.alerts.repo.AlertRepository;
import com.trackingworld.tw.alerts.service.AppSettingService;
import com.trackingworld.tw.alerts.repo.AlertTypeRepository;
import com.trackingworld.tw.alerts.repo.AppUserRepository;
import com.trackingworld.tw.alerts.repo.DiscardedRecipientRepository;
import com.trackingworld.tw.alerts.repo.IngestSourceRepository;
import com.trackingworld.tw.alerts.repo.NotificationDeliveryRepository;
import com.trackingworld.tw.alerts.repo.NotificationRepository;
import com.trackingworld.tw.alerts.repo.UserDeviceRepository;
import com.trackingworld.tw.common.util.PhoneNormalizer;
import com.trackingworld.tw.alerts.domain.Configuration;
import com.trackingworld.tw.alerts.repo.ConfigurationRepository;
import com.trackingworld.tw.ingest.dto.IngestConfigurationRequest;
import com.trackingworld.tw.common.ErrorCodes;
import com.trackingworld.tw.common.exceptions.BadRequestException;
import com.trackingworld.tw.ingest.dto.IngestAlertRequest;
import com.trackingworld.tw.ingest.dto.IngestResultDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * SPEC §6 step 1. Each item runs in its own transaction: normalise phones -> dedupe by
 * (source_id, source_alert_id) -> insert alert -> notification(PENDING) + delivery(QUEUED) per active device,
 * or discarded_recipient. Unknown alert types are auto-created as INFO. Dispatch is triggered after commit.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IngestService {

    private final AlertRepository alertRepository;
    private final AlertTypeRepository alertTypeRepository;
    private final ConfigurationRepository configurationRepository;
    private final AppSettingService appSettingService;
    private final AppUserRepository appUserRepository;
    private final UserDeviceRepository deviceRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationDeliveryRepository deliveryRepository;
    private final DiscardedRecipientRepository discardedRepository;
    private final IngestSourceRepository sourceRepository;
    private final TransactionTemplate transactionTemplate;
    private final ApplicationEventPublisher events;

    public List<IngestResultDto> ingest(Long sourceId, List<IngestAlertRequest> items) {
        List<IngestResultDto> results = new ArrayList<>(items.size());
        for (IngestAlertRequest item : items) {
            results.add(ingestOne(sourceId, item));
        }
        return results;
    }

    private IngestResultDto ingestOne(Long sourceId, IngestAlertRequest item) {
        long start = System.currentTimeMillis();
        try {
            IngestResultDto result = transactionTemplate.execute(status -> process(sourceId, item));
            log.info("[INGEST] source={} sourceAlertId={} uid={} type={} -> {} alertId={} accepted={} discarded={} in {} ms",
                    sourceId, item.sourceAlertId(), item.uid(), item.alertType(), result.result(), result.alertId(),
                    result.notified(), result.discarded(), System.currentTimeMillis() - start);
            return result;
        } catch (DataIntegrityViolationException e) {
            // Concurrent insert of the same (source, sourceAlertId) - treat as duplicate.
            log.info("[INGEST] source={} sourceAlertId={} -> DUPLICATE (race)", sourceId, item.sourceAlertId());
            Long existingId = alertRepository.findBySourceIdAndSourceAlertId(sourceId, item.sourceAlertId())
                    .map(Alert::getId).orElse(null);
            return duplicate(item, existingId);
        }
    }

    private IngestResultDto process(Long sourceId, IngestAlertRequest item) {
        Optional<Alert> existing = alertRepository.findBySourceIdAndSourceAlertId(sourceId, item.sourceAlertId().trim());
        if (existing.isPresent()) {
            return duplicate(item, existing.get().getId());
        }
        IngestSource source = sourceRepository.getReferenceById(sourceId);
        AlertType type = resolveType(item.alertType());
        String uid = item.uid().trim();

        // Who receives it: the Configuration table (uid + alert type, or '*' for all types).
        List<String> phones = configurationRepository.findPhones(uid);

        Alert alert = Alert.builder()
                .source(source)
                .sourceAlertId(item.sourceAlertId().trim())
                .uid(uid)
                .alertType(type)
                .message(item.message())
                .lat(item.lat())
                .lon(item.lon())
                .dbdt(item.dbdt() == null ? null : item.dbdt().toInstant())
                .sdt(Instant.now())
                .extra(item.extra() == null || item.extra().isEmpty() ? null : item.extra())
                .recipientsTotal(phones.size())
                .build();
        alert = alertRepository.save(alert);
        alert.setAlertTypeCode(type.getCode());

        List<DiscardedRecipient> discards = new ArrayList<>();
        List<IngestResultDto.DiscardedDto> discardedDtos = new ArrayList<>();
        int notified = 0;
        for (String e164 : phones) {
            Optional<AppUser> user = appUserRepository.findByPhoneE164(e164);
            if (user.isEmpty()) {
                discards.add(DiscardedRecipient.builder().alert(alert).rawPhone(e164).phoneE164(e164).reason(DiscardReason.NO_USER).build());
                discardedDtos.add(new IngestResultDto.DiscardedDto(e164, DiscardReason.NO_USER.name()));
                continue;
            }
            if (user.get().getStatus() != UserStatus.ACTIVE) {
                discards.add(DiscardedRecipient.builder().alert(alert).rawPhone(e164).phoneE164(e164).reason(DiscardReason.USER_DISABLED).build());
                discardedDtos.add(new IngestResultDto.DiscardedDto(e164, DiscardReason.USER_DISABLED.name()));
                continue;
            }
            createNotification(alert, user.get());
            notified++;
        }
        discardedRepository.saveAll(discards);
        alert.setRecipientsAccepted(notified);
        alert.setRecipientsDiscarded(discards.size());

        if (notified > 0) {
            events.publishEvent(new AlertAcceptedEvent(alert.getId()));
        }
        String result = phones.isEmpty() ? IngestResultDto.NO_CONFIGURATION : IngestResultDto.ACCEPTED;
        return new IngestResultDto(item.id(), alert.getId(), result, phones.size(), notified, discards.size(), discardedDtos);
    }

    /** Upsert one S3 Configuration row (keyed by S3's ID when given). Returns the stored row. */
    public Configuration saveConfiguration(IngestConfigurationRequest req) {
        String e164 = PhoneNormalizer.normalize(req.phoneNumber())
                .orElseThrow(() -> new BadRequestException(ErrorCodes.INVALID_PHONE));
        String type = req.alertType() == null || req.alertType().isBlank() || "*".equals(req.alertType().trim())
                ? Configuration.ALL_TYPES : req.alertType().trim().toUpperCase(Locale.ROOT);
        Instant now = Instant.now();
        Configuration cfg = (req.id() == null || req.id().isBlank() ? Optional.<Configuration>empty()
                : configurationRepository.findBySourceId(req.id().trim()))
                .orElseGet(() -> Configuration.builder().sourceId(req.id() == null || req.id().isBlank() ? null : req.id().trim())
                        .createdAt(now).build());
        cfg.setPhoneRaw(req.phoneNumber().trim());
        cfg.setPhoneE164(e164);
        cfg.setUid(req.uid().trim());
        cfg.setAlertType(type);
        cfg.setDbdt(req.dbdt() == null ? null : req.dbdt().toInstant());
        cfg.setCrtBy(req.crtBy());
        cfg.setUpdatedAt(now);
        Configuration saved = configurationRepository.save(cfg);
        log.info("[INGEST] configuration {} uid={} type={} phone={}", saved.getSourceId() == null ? "#" + saved.getId() : saved.getSourceId(),
                saved.getUid(), saved.getAlertType(), saved.getPhoneE164());
        return saved;
    }

    private void createNotification(Alert alert, AppUser user) {
        Notification notification = Notification.builder()
                .alert(alert)
                .user(user)
                .status(NotificationStatus.PENDING)
                .build();
        List<UserDevice> devices = deviceRepository.findActiveWithToken(user.getId());
        if (devices.isEmpty()) {
            notification.setStatus(NotificationStatus.NO_DEVICE);
        }
        notification = notificationRepository.save(notification);
        Instant now = Instant.now();
        for (UserDevice device : devices) {
            deliveryRepository.save(NotificationDelivery.builder()
                    .notification(notification)
                    .device(device)
                    .status(DeliveryStatus.QUEUED)
                    .queuedAt(now)
                    .nextAttemptAt(now)
                    .build());
        }
    }

    /**
     * Alert types are discovered dynamically: the first time S3 sends a code we have not seen, a catalogue row is
     * created with sensible defaults (severity from the keyword settings, matching channel/colour, humanised label).
     * Admins refine it in the dashboard; later alerts with the same code reuse the row.
     */
    private AlertType resolveType(String rawCode) {
        String code = rawCode.trim().toUpperCase(Locale.ROOT);
        return alertTypeRepository.findById(code).orElseGet(() -> {
            Severity severity = appSettingService.defaultSeverityFor(code);
            log.warn("[INGEST] new alertType '{}' discovered -> created as {} (edit it in the dashboard)", code, severity);
            return alertTypeRepository.save(AlertType.builder()
                    .code(code)
                    .label(humanise(code))
                    .severity(severity)
                    .color(defaultColor(severity))
                    .channelId(defaultChannel(severity))
                    .active(true)
                    .sortOrder(alertTypeRepository.maxSortOrder() + 10)
                    .build());
        });
    }

    static String humanise(String code) {
        String spaced = code.replace('_', ' ').replace('-', ' ').trim().toLowerCase(Locale.ROOT);
        return spaced.isEmpty() ? code : Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1);
    }

    private static String defaultColor(Severity severity) {
        return switch (severity) {
            case CRITICAL -> "#B3261E";
            case WARNING -> "#B26A00";
            case INFO -> "#0B57D0";
        };
    }

    private static String defaultChannel(Severity severity) {
        return switch (severity) {
            case CRITICAL -> "alerts_critical";
            case WARNING -> "alerts_warning";
            case INFO -> "alerts_default";
        };
    }

    private static IngestResultDto duplicate(IngestAlertRequest item, Long alertId) {
        return new IngestResultDto(item.id(), alertId, IngestResultDto.DUPLICATE, 0, 0, 0, List.of());
    }
}
