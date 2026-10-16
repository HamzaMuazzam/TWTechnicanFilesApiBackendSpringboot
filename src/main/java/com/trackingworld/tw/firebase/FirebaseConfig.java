package com.trackingworld.tw.firebase;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/** Initialises the Firebase Admin SDK from FIREBASE_CREDENTIALS_PATH, only when firebase.enabled=true. */
@Slf4j
@Configuration
@ConditionalOnProperty(prefix = "firebase", name = "enabled", havingValue = "true")
public class FirebaseConfig {

    @Bean
    public FirebaseApp firebaseApp(FirebaseProperties properties) throws IOException {
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }
        Path credentials = Path.of(properties.credentialsPath());
        if (!Files.isReadable(credentials)) {
            throw new IllegalStateException("firebase.enabled=true but FIREBASE_CREDENTIALS_PATH is not readable: "
                    + credentials.toAbsolutePath());
        }
        try (InputStream in = new FileInputStream(credentials.toFile())) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(in))
                    .build();
            FirebaseApp app = FirebaseApp.initializeApp(options);
            log.info("[FIREBASE] initialised project={} from {}", app.getOptions().getProjectId(), credentials.toAbsolutePath());
            return app;
        }
    }
}
