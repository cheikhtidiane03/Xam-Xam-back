package com.xaamxaam.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Badge obtenu par un eleve")
public record BadgeResponse(
        String code,
        String nom,
        String description,
        LocalDateTime dateObtention
) {
}
