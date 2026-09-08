package com.xaamxaam.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.mail")
public record MailProperties(
        String expediteur,
        String urlFrontend
) {
}
