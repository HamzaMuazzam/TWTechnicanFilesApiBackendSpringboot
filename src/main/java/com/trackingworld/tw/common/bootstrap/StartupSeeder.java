package com.trackingworld.tw.common.bootstrap;

import com.trackingworld.tw.alerts.domain.AdminRole;
import com.trackingworld.tw.alerts.domain.AdminUser;
import com.trackingworld.tw.alerts.domain.IngestSource;
import com.trackingworld.tw.alerts.repo.AdminUserRepository;
import com.trackingworld.tw.alerts.repo.IngestSourceRepository;
import com.trackingworld.tw.common.config.AppProperties;
import com.trackingworld.tw.security.ApiKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates the default admin user and the "S3" ingest source on first start.
 * Flyway seeds the static rows (alert types, settings); these two need hashed secrets from the environment.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StartupSeeder implements ApplicationRunner {

    public static final String DEFAULT_ADMIN_USERNAME = "admin";
    public static final String DEFAULT_ADMIN_PASSWORD = "ChangeMe123!";
    public static final String DEFAULT_SOURCE_NAME = "S3";

    private final AdminUserRepository adminUserRepository;
    private final IngestSourceRepository ingestSourceRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties appProperties;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedAdmin();
        seedIngestSource();
    }

    private void seedAdmin() {
        if (adminUserRepository.findByUsernameIgnoreCase(DEFAULT_ADMIN_USERNAME).isPresent()) {
            log.info("[SEED] admin user '{}' already exists", DEFAULT_ADMIN_USERNAME);
            return;
        }
        String password = appProperties.admin() != null && appProperties.admin().initialPassword() != null
                && !appProperties.admin().initialPassword().isBlank()
                ? appProperties.admin().initialPassword()
                : DEFAULT_ADMIN_PASSWORD;
        adminUserRepository.save(AdminUser.builder()
                .username(DEFAULT_ADMIN_USERNAME)
                .passwordHash(passwordEncoder.encode(password))
                .role(AdminRole.ADMIN)
                .active(true)
                .build());
        if (DEFAULT_ADMIN_PASSWORD.equals(password)) {
            log.warn("[SEED] *** created admin user '{}' with the DEFAULT password '{}'. Set ADMIN_INITIAL_PASSWORD "
                    + "before first start or change it from the dashboard immediately. ***", DEFAULT_ADMIN_USERNAME, DEFAULT_ADMIN_PASSWORD);
        } else {
            log.info("[SEED] created admin user '{}' with password from ADMIN_INITIAL_PASSWORD", DEFAULT_ADMIN_USERNAME);
        }
    }

    private void seedIngestSource() {
        if (ingestSourceRepository.findByName(DEFAULT_SOURCE_NAME).isPresent()) {
            log.info("[SEED] ingest source '{}' already exists", DEFAULT_SOURCE_NAME);
            return;
        }
        String configured = appProperties.ingest() != null ? appProperties.ingest().initialApiKey() : null;
        boolean generated = configured == null || configured.isBlank();
        String apiKey = generated ? ApiKeys.generate() : configured.trim();
        ingestSourceRepository.save(IngestSource.builder()
                .name(DEFAULT_SOURCE_NAME)
                .apiKeyHash(ApiKeys.hash(apiKey))
                .apiKeyPrefix(ApiKeys.prefix(apiKey))
                .active(true)
                .build());
        if (generated) {
            log.warn("[SEED] *** INGEST_INITIAL_API_KEY not set - generated API key for source '{}': {}  "
                    + "(shown ONCE - store it now, or rotate it from the dashboard) ***", DEFAULT_SOURCE_NAME, apiKey);
        } else {
            log.info("[SEED] created ingest source '{}' with key from INGEST_INITIAL_API_KEY (prefix {})",
                    DEFAULT_SOURCE_NAME, ApiKeys.prefix(apiKey));
        }
    }
}
