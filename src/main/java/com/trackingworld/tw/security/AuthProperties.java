package com.trackingworld.tw.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "auth")
public record AuthProperties(boolean devLoginEnabled) {
}
