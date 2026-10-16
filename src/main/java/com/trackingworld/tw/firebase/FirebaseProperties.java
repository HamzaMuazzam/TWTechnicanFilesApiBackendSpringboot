package com.trackingworld.tw.firebase;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "firebase")
public record FirebaseProperties(boolean enabled, String credentialsPath) {
}
