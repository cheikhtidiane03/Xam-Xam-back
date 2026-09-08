package com.xaamxaam.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.claude")
public record ClaudeApiProperties(
        String apiKey,
        String apiUrl,
        String model,
        String anthropicVersion,
        int maxTokens,
        int quotaRequetesJourEleve
) {
}
