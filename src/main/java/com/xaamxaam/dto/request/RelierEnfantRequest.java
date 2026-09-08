package com.xaamxaam.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Code de liaison genere par l'eleve, a saisir par le parent pour rattacher son compte")
public record RelierEnfantRequest(
        @NotBlank String code
) {
}
