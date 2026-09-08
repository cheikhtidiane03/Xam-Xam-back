package com.xaamxaam.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ocr")
public record OcrProperties(
        String apiKey,
        String apiUrl,
        String langue
) {
}
