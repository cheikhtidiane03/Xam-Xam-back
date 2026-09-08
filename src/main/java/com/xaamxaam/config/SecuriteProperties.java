package com.xaamxaam.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.securite")
public record SecuriteProperties(
        int maxTentativesConnexion,
        int fenetreBlocageMinutes
) {
}
