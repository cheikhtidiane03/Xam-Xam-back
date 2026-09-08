package com.xaamxaam.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Un indice debloque pour un exercice")
public record IndiceResponse(
        UUID id,
        int niveau,
        String contenu
) {
}
