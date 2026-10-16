package com.trackingworld.tw.alerts.service;

import com.trackingworld.tw.alerts.domain.AppSetting;
import com.trackingworld.tw.alerts.domain.Severity;
import com.trackingworld.tw.alerts.repo.AppSettingRepository;
import com.trackingworld.tw.common.ErrorCodes;
import com.trackingworld.tw.common.exceptions.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Typed access to app_setting. The table is tiny, so every read goes to the DB - no stale cache
 * after an admin PUT /settings.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppSettingService {

    public static final Set<String> INTEGER_KEYS = Set.of(
            AppSetting.RETENTION_DAYS, AppSetting.MAX_DEVICES_PER_USER, AppSetting.DISPATCH_MAX_ATTEMPTS);

    private final AppSettingRepository repository;

    @Transactional(readOnly = true)
    public int retentionDays() {
        return intValue(AppSetting.RETENTION_DAYS, 90);
    }

    @Transactional(readOnly = true)
    public int maxDevicesPerUser() {
        return intValue(AppSetting.MAX_DEVICES_PER_USER, 5);
    }

    @Transactional(readOnly = true)
    public int dispatchMaxAttempts() {
        return intValue(AppSetting.DISPATCH_MAX_ATTEMPTS, 5);
    }

    /** Default severity for a newly discovered alert type code, from the keyword settings. */
    @Transactional(readOnly = true)
    public Severity defaultSeverityFor(String code) {
        String upper = code.toUpperCase(Locale.ROOT);
        if (matchesAny(upper, AppSetting.SEVERITY_KEYWORDS_CRITICAL)) {
            return Severity.CRITICAL;
        }
        if (matchesAny(upper, AppSetting.SEVERITY_KEYWORDS_WARNING)) {
            return Severity.WARNING;
        }
        return Severity.INFO;
    }

    private boolean matchesAny(String upperCode, String settingKey) {
        return repository.findById(settingKey)
                .map(AppSetting::getValue)
                .map(v -> Arrays.stream(v.split(","))
                        .map(String::trim)
                        .filter(k -> !k.isEmpty())
                        .anyMatch(k -> upperCode.contains(k.toUpperCase(Locale.ROOT))))
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public Map<String, String> all() {
        Map<String, String> out = new LinkedHashMap<>();
        repository.findAll().stream()
                .sorted((a, b) -> a.getKey().compareTo(b.getKey()))
                .forEach(s -> out.put(s.getKey(), s.getValue()));
        return out;
    }

    @Transactional
    public Map<String, String> update(Map<String, String> values) {
        values.forEach((key, value) -> {
            if (key == null || key.isBlank() || value == null) {
                throw new BadRequestException(ErrorCodes.INVALID_SETTING);
            }
            String trimmed = value.trim();
            if (INTEGER_KEYS.contains(key)) {
                try {
                    if (Integer.parseInt(trimmed) < 1) {
                        throw new BadRequestException(ErrorCodes.INVALID_SETTING);
                    }
                } catch (NumberFormatException e) {
                    throw new BadRequestException(ErrorCodes.INVALID_SETTING);
                }
            }
            AppSetting setting = repository.findById(key)
                    .orElseGet(() -> AppSetting.builder().key(key).build());
            setting.setValue(trimmed);
            setting.setUpdatedAt(Instant.now());
            repository.save(setting);
            log.info("[SETTINGS] {} = {}", key, trimmed);
        });
        return all();
    }

    private int intValue(String key, int defaultValue) {
        return repository.findById(key)
                .map(AppSetting::getValue)
                .map(v -> {
                    try {
                        return Integer.parseInt(v.trim());
                    } catch (NumberFormatException e) {
                        log.warn("[SETTINGS] {}='{}' is not an integer, using default {}", key, v, defaultValue);
                        return defaultValue;
                    }
                })
                .orElse(defaultValue);
    }
}
