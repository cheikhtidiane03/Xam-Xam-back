package com.xaamxaam.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Texte extrait d'une photo d'exercice via OCR")
public record OcrResponse(
        String texteExtrait
) {
}
