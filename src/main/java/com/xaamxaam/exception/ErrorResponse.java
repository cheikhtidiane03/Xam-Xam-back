package com.xaamxaam.exception;

import java.time.LocalDateTime;
import java.util.List;

public record ErrorResponse(
        LocalDateTime horodatage,
        int statut,
        String erreur,
        String message,
        List<String> details
) {
}
