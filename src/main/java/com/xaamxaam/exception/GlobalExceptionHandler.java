package com.xaamxaam.exception;

import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.List;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler({ResourceNotFoundException.class, EntityNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleNotFound(RuntimeException ex) {
        return build(HttpStatus.NOT_FOUND, "Ressource introuvable", ex.getMessage(), null);
    }

    @ExceptionHandler({AccesRefuseException.class, AccessDeniedException.class})
    public ResponseEntity<ErrorResponse> handleAccesRefuse(RuntimeException ex) {
        return build(HttpStatus.FORBIDDEN, "Acces refuse", ex.getMessage(), null);
    }

    @ExceptionHandler(QuotaDepasseException.class)
    public ResponseEntity<ErrorResponse> handleQuotaDepasse(QuotaDepasseException ex) {
        return build(HttpStatus.TOO_MANY_REQUESTS, "Quota depasse", ex.getMessage(), null);
    }

    @ExceptionHandler(TokenInvalideException.class)
    public ResponseEntity<ErrorResponse> handleTokenInvalide(TokenInvalideException ex) {
        return build(HttpStatus.BAD_REQUEST, "Token invalide", ex.getMessage(), null);
    }

    @ExceptionHandler(CompteTemporairementBloqueException.class)
    public ResponseEntity<ErrorResponse> handleCompteBloque(CompteTemporairementBloqueException ex) {
        return build(HttpStatus.TOO_MANY_REQUESTS, "Connexion temporairement bloquee", ex.getMessage(), null);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex) {
        return build(HttpStatus.UNAUTHORIZED, "Identifiants invalides", "Email ou mot de passe incorrect", null);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return build(HttpStatus.BAD_REQUEST, "Requete invalide", ex.getMessage(), null);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex) {
        log.warn("IllegalStateException geree : {}", ex.getMessage(), ex);
        return build(HttpStatus.SERVICE_UNAVAILABLE, "Service indisponible", ex.getMessage(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .toList();
        return build(HttpStatus.BAD_REQUEST, "Erreur de validation", "Certains champs sont invalides", details);
    }

    /**
     * Filet de securite pour toute exception non geree explicitement
     * ailleurs. IMPORTANT : le corps de la reponse HTTP reste volontairement
     * generique (on ne veut jamais exposer de stack trace ou de details
     * internes au client), MAIS l'exception complete est desormais loguee
     * cote serveur avec sa stack trace - sans ce log, ces erreurs etaient
     * jusqu'ici invisibles autant cote client QUE cote serveur, rendant
     * tout diagnostic impossible.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        log.error("Exception non geree : {}", ex.getMessage(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur interne", "Une erreur inattendue est survenue", null);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String erreur, String message, List<String> details) {
        ErrorResponse body = new ErrorResponse(LocalDateTime.now(), status.value(), erreur, message, details);
        return ResponseEntity.status(status).body(body);
    }
}
