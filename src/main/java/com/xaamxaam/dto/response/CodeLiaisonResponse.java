package com.xaamxaam.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Code temporaire a partager avec son parent pour qu'il rattache son compte")
public record CodeLiaisonResponse(
        String code,
        LocalDateTime dateExpiration
) {
}
